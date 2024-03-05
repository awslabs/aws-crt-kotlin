/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.*
import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.util.ShutdownChannel
import aws.sdk.kotlin.crt.util.shutdownChannel
import kotlinx.cinterop.*
import libcrt.*

@OptIn(ExperimentalForeignApi::class)
public actual class HostResolver actual constructor(elg: EventLoopGroup, maxEntries: Int) : NativeHandle<aws_host_resolver>, Closeable, AsyncShutdown {
    public actual constructor(elg: EventLoopGroup) : this(elg, DEFAULT_MAX_ENTRIES)

    override val ptr: CPointer<aws_host_resolver>

    private val shutdownCompleteChannel = shutdownChannel()
    private val channelStableRef = StableRef.create(shutdownCompleteChannel)

    init {
        ptr = memScoped {
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
                "aws_host_resolver_new_default()"
            }
        }
    }

    override suspend fun waitForShutdown() {
        shutdownCompleteChannel.receive()
    }

    override fun close() {
        aws_host_resolver_release(ptr)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun onShutdownComplete(userData: COpaquePointer?) {
    if (userData == null) return
    val stableRef = userData.asStableRef<ShutdownChannel>()
    val ch = stableRef.get()
    ch.trySend(Unit)
    ch.close()
    stableRef.dispose()
}
