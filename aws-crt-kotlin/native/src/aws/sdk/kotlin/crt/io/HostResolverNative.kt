/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.*
import aws.sdk.kotlin.crt.Allocator
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import libcrt.*

@OptIn(ExperimentalForeignApi::class)
public actual class HostResolver actual constructor(elg: EventLoopGroup, maxEntries: Int) : CrtResource<aws_host_resolver>(), Closeable, AsyncShutdown {
    public actual constructor(elg: EventLoopGroup) : this(elg, DEFAULT_MAX_ENTRIES)

    private val resolver: CPointer<aws_host_resolver>
    override val ptr: CPointer<aws_host_resolver>
        get() = resolver

    private val shutdownCompleteChannel = Channel<Unit>(Channel.RENDEZVOUS)
    private val channelStableRef = StableRef.create(shutdownCompleteChannel)

    init {
        resolver = memScoped {
            val shutdownOpts = cValue<aws_shutdown_callback_options> {
                shutdown_callback_fn = staticCFunction(::onShutdownComplete)
                shutdown_callback_user_data = channelStableRef.asCPointer()
            }

            val resolverOpts = cValue<aws_host_resolver_default_options> {
                el_group = elg.ptr
                shutdown_options = shutdownOpts.ptr
                max_entries = maxEntries.convert()
            }

            checkNotNull(aws_host_resolver_new_default(Allocator.Default, resolverOpts)) {
                "aws_host_resolver_new_default() failed"
            }
        }
    }

    override suspend fun waitForShutdown() {
        // FIXME What needs to happen here
    }

    override fun close() {
        aws_host_resolver_release(resolver)
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