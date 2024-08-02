/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.server.netty

import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.test.base.*
import io.netty.channel.*
import kotlin.test.*

abstract class NettyCustomChannelTest<TEngine : ServerEngine, TConfiguration : ServerEngine.Configuration>(
    hostFactory: ServerEngineFactory<TEngine, TConfiguration>
) : EngineTestBase<TEngine, TConfiguration>(hostFactory) {

    var counter = 0

    @Test
    fun testCustomChannelHandlerInvoked() {
        createAndStartServer {
            handle {
                call.respondText("Hello")
            }
        }

        withUrl("/") {
            assertEquals(HttpStatusCode.OK.value, status.value)
            assertNotEquals(0, counter)
        }
    }
}

class NettyCustomChannelPipelineConfigurationTest :
    NettyCustomChannelTest<NettyServerEngine, NettyServerEngine.Configuration>(Netty) {

    override fun configure(configuration: NettyServerEngine.Configuration) {
        configuration.shareWorkGroup = true
        configuration.channelPipelineConfig = {
            addLast(
                "customHandler",
                object : ChannelInboundHandlerAdapter() {
                    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                        counter = counter.plus(1)
                        super.channelRead(ctx, msg)
                    }
                }
            )
        }
    }
}
