/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.konan.target.*

val releaseVersion: String? by extra
val eapVersion: String? by extra
val version = (project.version as String).let { if (it.endsWith("-SNAPSHOT")) it.dropLast("-SNAPSHOT".length) else it }

extra["configuredVersion"] = when {
    releaseVersion != null -> releaseVersion
    eapVersion != null -> "$version-eap-$eapVersion"
    else -> project.version
}

println("The build version is ${extra["configuredVersion"]}")

extra["globalM2"] = "${project.file("build")}/m2"
extra["publishLocal"] = project.hasProperty("publishLocal")

val configuredVersion: String by extra

apply(from = "gradle/verifier.gradle")

extra["skipPublish"] = mutableListOf(
    "ktor-server-test-suites",
    "ktor-server-tests",
    "ktor-junit",
)

// Point old artifact to new location
extra["relocatedArtifacts"] = mapOf(
    "ktor-server-test-base" to "ktor-server-test-host",
)

extra["nonDefaultProjectStructure"] = mutableListOf(
    "ktor-bom",
    "ktor-java-modules-test",
)

val disabledExplicitApiModeProjects = listOf(
    "ktor-client-tests",
    "ktor-server-test-base",
    "ktor-server-test-suites",
    "ktor-server-tests",
    "ktor-client-content-negotiation-tests",
    "ktor-junit"
)

apply(from = "gradle/compatibility.gradle")

plugins {
    id("org.jetbrains.dokka") version "1.9.20" apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.16.3"
    id("com.osacky.doctor") version "0.10.0"
}

doctor {
    enableTestCaching = false
}

subprojects {
    group = "io.ktor"
    version = configuredVersion
    extra["hostManager"] = HostManager()

    setupTrainForSubproject()

    val nonDefaultProjectStructure: List<String> by rootProject.extra
    if (nonDefaultProjectStructure.contains(project.name)) return@subprojects

    apply(plugin = "kotlin-multiplatform")
    apply(plugin = "atomicfu-conventions")

    configureTargets()

    configurations {
        maybeCreate("testOutput")
    }

    kotlin {
        if (!disabledExplicitApiModeProjects.contains(project.name)) explicitApi()

        configureSourceSets()
        setupJvmToolchain()
    }

    val skipPublish: List<String> by rootProject.extra
    if (!skipPublish.contains(project.name)) {
        configurePublication()
    }

    configureCodestyle()
}

println("Using Kotlin compiler version: ${libs.versions.kotlin.get()}")
filterSnapshotTests()

fun configureDokka() {
    allprojects {
        plugins.apply("org.jetbrains.dokka")

        val dokkaPlugin by configurations
        dependencies {
            dokkaPlugin("org.jetbrains.dokka:versioning-plugin:1.9.20")
        }
    }

    val dokkaOutputDir = "../versions"

    tasks.withType<DokkaMultiModuleTask>().configureEach {
        val id = "org.jetbrains.dokka.versioning.VersioningPlugin"
        val config = """{ "version": "$configuredVersion", "olderVersionsDir":"$dokkaOutputDir" }"""
        val mapOf = mapOf(id to config)

        outputDirectory.set(file(projectDir.toPath().resolve(dokkaOutputDir).resolve(configuredVersion)))
        pluginsMapConfiguration.set(mapOf)
    }

    rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().ignoreScripts = false
    }
}

configureDokka()

fun Project.setupJvmToolchain() {
    val jdk = when (project.name) {
        in jdk11Modules -> 11
        else -> 8
    }

    kotlin {
        jvmToolchain(jdk)
    }
}

subprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        configureCompilerOptions()
    }
}

fun KotlinMultiplatformExtension.configureSourceSets() {
    sourceSets
        .matching { it.name !in listOf("main", "test") }
        .all {
            val srcDir = if (name.endsWith("Main")) "src" else "test"
            val resourcesPrefix = if (name.endsWith("Test")) "test-" else ""
            val platform = name.dropLast(4)

            kotlin.srcDir("$platform/$srcDir")
            resources.srcDir("$platform/${resourcesPrefix}resources")

            languageSettings.apply {
                progressiveMode = true
            }
        }
}
