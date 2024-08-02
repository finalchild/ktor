/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.cio

import io.ktor.server.config.*
import io.ktor.server.engine.*
import kotlin.jvm.*

/**
 * Default engine with main function that starts CIO engine using application.conf
 */
public object EngineMain {
    /**
     * CIO engine entry point
     */
    @JvmStatic
    public fun main(args: Array<String>) {
        val config = CommandLineConfig(args)
        val server = EmbeddedServer(config.serverParameters, CIO) {
            takeFrom(config.engineConfig)
            loadConfiguration(config.serverParameters.environment.config)
        }
        server.start(true)
    }

    private fun CIOServerEngine.Configuration.loadConfiguration(config: ServerConfig) {
        val deploymentConfig = config.config("ktor.deployment")
        loadCommonConfiguration(deploymentConfig)
        deploymentConfig.propertyOrNull("connectionIdleTimeoutSeconds")?.getString()?.toInt()?.let {
            connectionIdleTimeoutSeconds = it
        }
    }
}
