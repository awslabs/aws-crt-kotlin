/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.*
import aws.sdk.kotlin.crt.Allocator
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import libcrt.aws_client_bootstrap
import libcrt.aws_client_bootstrap_new
import libcrt.aws_client_bootstrap_options
import libcrt.aws_client_bootstrap_release

@OptIn(ExperimentalForeignApi::class)
public actual class ClientBootstrap actual constructor(
    elg: EventLoopGroup,
    hr: HostResolver,
) : CrtResource<aws_client_bootstrap>(), Closeable, AsyncShutdown {
    private val bootstrap: CPointer<aws_client_bootstrap>
    private val shutdownCompleteChannel = Channel<Unit>(Channel.RENDEZVOUS)
    private val channelStableRef = StableRef.create(shutdownCompleteChannel)

    override val ptr: CPointer<aws_client_bootstrap>
        get() = bootstrap

    init {
        val opts = cValue<aws_client_bootstrap_options> {
            event_loop_group = elg.ptr
            host_resolver = hr.ptr
            on_shutdown_complete = staticCFunction(::onShutdownComplete)
            user_data = channelStableRef.asCPointer()
        }

        bootstrap = checkNotNull(aws_client_bootstrap_new(Allocator.Default, opts)) {
            "aws_client_bootstrap_new() failed"
        }
    }

    override suspend fun waitForShutdown() {
        // FIXME What needs to happen here? Does anything need to happen?
    }

    override fun close() {
        aws_client_bootstrap_release(bootstrap)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun onShutdownComplete(userData: COpaquePointer?) {
    if (userData != null) {
        val shutdownCompleteChannel = userData.asStableRef<Channel<Unit>>().get()
        shutdownCompleteChannel.trySend(Unit)
        shutdownCompleteChannel.close()
    }
}
