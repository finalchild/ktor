/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.server.jetty.http2.jakarta

import io.ktor.server.jetty.jakarta.*
import io.ktor.server.testing.suites.*
import kotlin.test.*

class JettyHttp2BlockingServletContainerCompressionTest :
    CompressionTestSuite<JettyServerEngineBase, JettyServerEngineBase.Configuration>(Servlet(async = false))

class JettyHttp2BlockingServletContainerContentTest :
    ContentTestSuite<JettyServerEngineBase, JettyServerEngineBase.Configuration>(Servlet(async = false))

class JettyHttp2BlockingServletContainerHttpServerCommonTest :
    HttpServerCommonTestSuite<JettyServerEngineBase, JettyServerEngineBase.Configuration>(
        Servlet(async = false)
    ) {
    override fun testFlushingHeaders() {
        // no op
    }
}

class JettyHttp2BlockingServletContainerHttpServerJvmTest :
    HttpServerJvmTestSuite<JettyServerEngineBase, JettyServerEngineBase.Configuration>(
        Servlet(async = false)
    ) {

    @Ignore
    override fun testUpgrade() {
    }

    @Ignore
    override fun testPipelining() {
    }

    @Ignore
    override fun testPipeliningWithFlushingHeaders() {
    }
}

class JettyHttp2BlockingServletContainerSustainabilityTest :
    SustainabilityTestSuite<JettyServerEngineBase, JettyServerEngineBase.Configuration>(
        Servlet(async = false)
    )
