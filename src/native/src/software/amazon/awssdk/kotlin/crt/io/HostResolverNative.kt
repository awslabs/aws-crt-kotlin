/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import kotlinx.cinterop.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.receiveOrNull
import libcrt.aws_host_resolver
import libcrt.aws_host_resolver_new_default
import libcrt.aws_host_resolver_release
import libcrt.aws_shutdown_callback_options
import software.amazon.awssdk.kotlin.crt.*
import software.amazon.awssdk.kotlin.crt.Allocator
import software.amazon.awssdk.kotlin.crt.util.ShutdownChannel
import software.amazon.awssdk.kotlin.crt.util.shutdownChannel
import kotlin.native.concurrent.freeze

public actual class HostResolver actual constructor(
    elg: EventLoopGroup,
    maxEntries: Int
) : CrtResource<aws_host_resolver>(), Closeable {

    private val resolver: CPointer<aws_host_resolver>
    private val shutdownComplete: ShutdownChannel = shutdownChannel().freeze()
    private val stableRef = StableRef.create(shutdownComplete)

    init {
        val shutdownOpts = cValue<aws_shutdown_callback_options> {
            shutdown_callback_fn = staticCFunction(::onShutdownComplete)
            shutdown_callback_user_data = stableRef.asCPointer()
        }

        resolver = awsAssertNotNull(
            aws_host_resolver_new_default(Allocator.Default, maxEntries.convert(), elg, shutdownOpts)
        ) { "aws_host_resolver_new_default()" }
    }

    public actual constructor(elg: EventLoopGroup) : this(elg, DEFAULT_MAX_ENTRIES)

    override val ptr: CPointer<aws_host_resolver>
        get() = resolver

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun close() {
        aws_host_resolver_release(resolver)
        shutdownComplete.receiveOrNull()
        stableRef.dispose()
    }
}

private fun onShutdownComplete(userdata: COpaquePointer?) {
    if (userdata != null) {
        initRuntimeIfNeeded()
        val notify = userdata.asStableRef<ShutdownChannel>().get()
        notify.offer(Unit)
        notify.close()
    }
}
