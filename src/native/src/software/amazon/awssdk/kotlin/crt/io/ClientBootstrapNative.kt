/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import kotlinx.cinterop.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.receiveOrNull
import libcrt.aws_client_bootstrap
import libcrt.aws_client_bootstrap_new
import libcrt.aws_client_bootstrap_options
import libcrt.aws_client_bootstrap_release
import software.amazon.awssdk.kotlin.crt.Allocator
import software.amazon.awssdk.kotlin.crt.CrtResource
import software.amazon.awssdk.kotlin.crt.CrtRuntimeException
import kotlin.native.concurrent.freeze

private fun onShutdownComplete(userdata: COpaquePointer?) {
    if (userdata != null) {
        val notify = userdata.asStableRef<Channel<Unit>>().get()
        notify.offer(Unit)
        notify.close()
    }
}

actual class ClientBootstrap actual constructor(
    elg: EventLoopGroup,
    hr: HostResolver
) : CrtResource<aws_client_bootstrap>() {

    private val bootstrap: CPointer<aws_client_bootstrap>
    private val shutdownComplete = Channel<Unit>(0).freeze()
    private val stableRef = StableRef.create(shutdownComplete)

    init {
        val opts = cValue<aws_client_bootstrap_options> {
            event_loop_group = elg.ptr
            host_resolver = hr.ptr
            on_shutdown_complete = staticCFunction(::onShutdownComplete)
            user_data = stableRef.asCPointer()
        }

        bootstrap = aws_client_bootstrap_new(Allocator.Default, opts) ?: throw CrtRuntimeException("aws_client_bootstrap_new() failed")
    }

    override val ptr: CPointer<aws_client_bootstrap>
        get() = bootstrap

    @OptIn(ExperimentalCoroutinesApi::class)
    actual suspend fun close() {
        aws_client_bootstrap_release(bootstrap)
        shutdownComplete.receiveOrNull()
    }
}
