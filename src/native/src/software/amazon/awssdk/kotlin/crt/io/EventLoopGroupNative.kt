/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import kotlinx.cinterop.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.receiveOrNull
import libcrt.*
import software.amazon.awssdk.kotlin.crt.Allocator
import software.amazon.awssdk.kotlin.crt.Closeable
import software.amazon.awssdk.kotlin.crt.CrtResource
import software.amazon.awssdk.kotlin.crt.CrtRuntimeException
import software.amazon.awssdk.kotlin.crt.util.ShutdownChannel
import software.amazon.awssdk.kotlin.crt.util.shutdownChannel
import kotlin.native.concurrent.freeze

/**
 * Creates a new event loop group for the I/O subsystem to use to run blocking I/O requests
 * This class wraps the aws_event_loop_group from aws-c-io
 *
 * @param numThreads The number of threads that the event loop group may run tasks across. Usually 1.
 * @throws [software.amazon.awssdk.kotlin.crt.CrtRuntimeException] If the system is unable to allocate space for a native event loop group
 */
@OptIn(ExperimentalUnsignedTypes::class)
public actual class EventLoopGroup actual constructor(numThreads: Int) : CrtResource<aws_event_loop_group>(), Closeable {
    private val elg: CPointer<aws_event_loop_group>
    private val shutdownComplete: ShutdownChannel = shutdownChannel().freeze()
    private val stableRef = StableRef.create(shutdownComplete)

    init {
        val shutdownOpts = cValue<aws_shutdown_callback_options> {
            shutdown_callback_fn = staticCFunction(::onShutdownComplete)
            shutdown_callback_user_data = stableRef.asCPointer()
        }

        elg = aws_event_loop_group_new_default(Allocator.Default, numThreads.toUShort(), shutdownOpts) ?: throw CrtRuntimeException("aws_event_loop_group_new_default() failed")
    }

    override val ptr: CPointer<aws_event_loop_group>
        get() = elg

    /**
     * Close this ELG
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun close() {
        aws_event_loop_group_release(elg)
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
