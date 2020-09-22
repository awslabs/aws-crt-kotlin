/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import kotlinx.cinterop.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.receiveOrNull
import libcrt.aws_event_loop_group
import libcrt.aws_event_loop_group_new_default
import libcrt.aws_event_loop_group_release
import libcrt.aws_shutdown_callback_options
import software.amazon.awssdk.kotlin.crt.Allocator
import software.amazon.awssdk.kotlin.crt.CrtRuntimeException

private fun onShutdownComplete(userdata: COpaquePointer?) {
    if (userdata != null) {
        val elg = userdata.asStableRef<EventLoopGroup>().get()
        // free the waiter
        elg.shutdownComplete.offer(Unit)
        elg.shutdownComplete.close()
    }
}

/**
 * Creates a new event loop group for the I/O subsystem to use to run blocking I/O requests
 * This class wraps the aws_event_loop_group from aws-c-io
 *
 * @param numThreads The number of threads that the event loop group may run tasks across. Usually 1.
 * @throws [software.amazon.awssdk.kotlin.crt.CrtRuntimeException] If the system is unable to allocate space for a native event loop group
 */
actual class EventLoopGroup actual constructor(numThreads: Int) {
    private val elg: CPointer<aws_event_loop_group>
    internal val shutdownComplete = Channel<Unit>(0)
    private val stableRef = StableRef.create(this)

    init {
        val shutdownOpts = cValue<aws_shutdown_callback_options> {
            shutdown_callback_fn = staticCFunction(::onShutdownComplete)
            shutdown_callback_user_data = stableRef.asCPointer()
        }

        elg = aws_event_loop_group_new_default(Allocator.DEFAULT, numThreads.toUShort(), shutdownOpts) ?: throw CrtRuntimeException("aws_event_loop_group_new_default() failed")
    }

    actual companion object {
        actual val DEFAULT: EventLoopGroup by lazy {
            EventLoopGroup(1)
        }
    }

    /**
     * Close this ELG
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    actual suspend fun close() {
        aws_event_loop_group_release(elg)
        shutdownComplete.receiveOrNull()
        stableRef.dispose()
    }
}
