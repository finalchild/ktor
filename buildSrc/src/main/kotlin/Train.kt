/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
import internal.*
import org.gradle.api.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.testing.*
import org.gradle.kotlin.dsl.*

fun Project.filterSnapshotTests() {
    if (!buildSnapshotTrain) return

    println("Hacking test tasks, removing stress and flaky tests")
    subprojects {
        tasks.withType<Test>().configureEach {
            exclude("**/*ServerSocketTest*")
            exclude("**/*NettyStressTest*")
            exclude("**/*CIOMultithreadedTest*")
            exclude("**/*testBlockingConcurrency*")
            exclude("**/*testBigFile*")
            exclude("**/*numberTest*")
            exclude("**/*testWithPause*")
            exclude("**/*WebSocketTest*")
            exclude("**/*PostTest*")
            exclude("**/*testCustomUrls*")
            exclude("**/*testStaticServeFromDir*")
            exclude("**/*testRedirect*")
            exclude("**/*CIOHttpsTest*")
        }
    }

    println("Manifest of kotlin-compiler-embeddable.jar")

    subprojects.filter { it.name == "ktor-client" }.forEach {
        configurations.matching { it.name == "kotlinCompilerClasspath" }.all {
            resolvedConfiguration.files.filter { it.name.contains("kotlin-compiler-embeddable") }.forEach {
                val manifest = zipTree(it).matching {
                    include("META-INF/MANIFEST.MF")
                }.files.first()

                manifest.readLines().forEach {
                    println(it)
                }
            }
        }
    }
}

fun Project.setupTrainForSubproject() {
    if (!buildSnapshotTrain) return

    val kotlinVersion = rootProject.libs.versions.kotlin.get()
    println("Using Kotlin $kotlinVersion for project $this")

    val deployVersion = properties["DeployVersion"]
    if (deployVersion != null) version = deployVersion

    if (!rootProject.hasProperty("skip_snapshot_checks")) {
        check(version, rootProject.libs.versions.atomicfu, "atomicfu")
        check(version, rootProject.libs.versions.coroutines, "coroutines")
        check(version, rootProject.libs.versions.serialization, "serialization")
    }
}

private val Project.buildSnapshotTrain: Boolean
    get() = rootProject.findProperty("build_snapshot_train")?.toString().toBoolean()

private fun check(version: Any, libVersion: Provider<String>, libName: String) {
    check(version == libVersion) {
        "Current deploy version is $version, but $libName version is not overridden (${libVersion.get()})"
    }
}
