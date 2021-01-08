/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import kotlinx.cinterop.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.receiveOrNull
import libcrt.*
import software.amazon.awssdk.kotlin.crt.*
import software.amazon.awssdk.kotlin.crt.Allocator
import software.amazon.awssdk.kotlin.crt.util.ShutdownChannel
import software.amazon.awssdk.kotlin.crt.util.shutdownChannel
import kotlin.native.concurrent.freeze

public actual class HostResolver actual constructor(
    elg: EventLoopGroup,
    maxEntries: Int
) : CrtResource<aws_host_resolver>(), Closeable, AsyncShutdown {

    private val resolver: CPointer<aws_host_resolver>
    private val shutdownComplete: ShutdownChannel = shutdownChannel().freeze()
    private val stableRef = StableRef.create(shutdownComplete)

    init {
        resolver = memScoped {
            val shutdownOpts = cValue<aws_shutdown_callback_options> {
                shutdown_callback_fn = staticCFunction(::onShutdownComplete)
                shutdown_callback_user_data = stableRef.asCPointer()
            }

            val resolverOpts = cValue<aws_host_resolver_default_options> {
                el_group = elg.ptr
                shutdown_options = shutdownOpts.ptr
                max_entries = maxEntries.convert()
            }

            awsAssertNotNull(
                aws_host_resolver_new_default(Allocator.Default, resolverOpts)
            ) { "aws_host_resolver_new_default()" }
        }
    }

    public actual constructor(elg: EventLoopGroup) : this(elg, DEFAULT_MAX_ENTRIES)

    override val ptr: CPointer<aws_host_resolver>
        get() = resolver

    override fun close() {
        aws_host_resolver_release(resolver)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun waitForShutdown() {
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
