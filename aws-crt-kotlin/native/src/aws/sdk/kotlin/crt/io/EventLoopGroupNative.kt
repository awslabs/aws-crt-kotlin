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

/**
 * Creates a new event loop group for the I/O subsystem to use to run blocking I/O requests
 * This class wraps the aws_event_loop_group from aws-c-io
 *
 * @param maxThreads If maxThreads == 0, then the loop count will be the number of available processors on the machine.
 * Otherwise, maxThreads will be the number of event loops in the group.
 * @throws [aws.sdk.kotlin.crt.CrtRuntimeException] If the system is unable to allocate space for a native event loop group
 */
@OptIn(ExperimentalForeignApi::class)
public actual class EventLoopGroup actual constructor(maxThreads: Int) : CrtResource<aws_event_loop_group>(), Closeable, AsyncShutdown {
    private val elg: CPointer<aws_event_loop_group>

    override val ptr: CPointer<aws_event_loop_group>
        get() = elg

    private val shutdownCompleteChannel = Channel<Unit>(Channel.RENDEZVOUS)
    private val channelStableRef = StableRef.create(shutdownCompleteChannel)

    init {
        val shutdownOpts = cValue<aws_shutdown_callback_options> {
            shutdown_callback_fn = staticCFunction(::onShutdownComplete)
            shutdown_callback_user_data = channelStableRef.asCPointer()
        }

        elg = checkNotNull(aws_event_loop_group_new_default(Allocator.Default, maxThreads.toUShort(), shutdownOpts)) {
            "aws_event_loop_group_new_default()"
        }
    }

    override suspend fun waitForShutdown() {
        shutdownCompleteChannel.receive()
        channelStableRef.dispose()
    }

    override fun close() {
        aws_event_loop_group_release(elg)
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
