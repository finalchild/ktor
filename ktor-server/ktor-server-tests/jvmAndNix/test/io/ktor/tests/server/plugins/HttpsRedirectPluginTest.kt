/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.*

class HttpsRedirectPluginTest {
    @Test
    fun testRedirect() {
        withTestApplication {
            server.install(HttpsRedirect)
            server.routing {
                get("/") {
                    call.respond("ok")
                }
            }

            handleRequest(HttpMethod.Get, "/").let { call ->
                assertEquals(HttpStatusCode.MovedPermanently, call.response.status())
                assertEquals("https://localhost/", call.response.headers[HttpHeaders.Location])
            }
        }
    }

    @Test
    fun testRedirectHttps() {
        withTestApplication {
            server.install(XForwardedHeaders)
            server.install(HttpsRedirect)
            server.routing {
                get("/") {
                    call.respond("ok")
                }
            }

            handleRequest(HttpMethod.Get, "/") {
                addHeader(HttpHeaders.XForwardedProto, "https")
            }.let { call ->
                assertEquals(HttpStatusCode.OK, call.response.status())
            }
        }
    }

    @Test
    fun testDirectPathAndQuery() {
        withTestApplication {
            server.install(HttpsRedirect)
            server.intercept(ServerCallPipeline.Fallback) {
                call.respond("ok")
            }

            handleRequest(HttpMethod.Get, "/some/path?q=1").let { call ->
                assertEquals(HttpStatusCode.MovedPermanently, call.response.status())
                assertEquals("https://localhost/some/path?q=1", call.response.headers[HttpHeaders.Location])
            }
        }
    }

    @Test
    fun testDirectPathAndQueryWithCustomPort() {
        withTestApplication {
            server.install(HttpsRedirect) {
                sslPort = 8443
            }
            server.intercept(ServerCallPipeline.Fallback) {
                call.respond("ok")
            }

            handleRequest(HttpMethod.Get, "/some/path?q=1").let { call ->
                assertEquals(HttpStatusCode.MovedPermanently, call.response.status())
                assertEquals("https://localhost:8443/some/path?q=1", call.response.headers[HttpHeaders.Location])
            }
        }
    }

    @Test
    fun testRedirectHttpsPrefixExemption() {
        withTestApplication {
            server.install(HttpsRedirect) {
                excludePrefix("/exempted")
            }
            server.routing {
                get("/exempted/path") {
                    call.respond("ok")
                }
            }

            handleRequest(HttpMethod.Get, "/nonexempted").let { call ->
                assertEquals(HttpStatusCode.MovedPermanently, call.response.status())
            }

            handleRequest(HttpMethod.Get, "/exempted/path").let { call ->
                assertEquals(HttpStatusCode.OK, call.response.status())
            }
        }
    }

    @Test
    fun testRedirectHttpsSuffixExemption() {
        withTestApplication {
            server.install(HttpsRedirect) {
                excludeSuffix("exempted")
            }
            server.routing {
                get("/path/exempted") {
                    call.respond("ok")
                }
            }

            handleRequest(HttpMethod.Get, "/exemptednot").let { call ->
                assertEquals(HttpStatusCode.MovedPermanently, call.response.status())
            }

            handleRequest(HttpMethod.Get, "/path/exempted").let { call ->
                assertEquals(HttpStatusCode.OK, call.response.status())
            }
        }
    }
}
