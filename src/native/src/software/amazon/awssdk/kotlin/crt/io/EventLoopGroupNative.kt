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

/**
 * Creates a new event loop group for the I/O subsystem to use to run blocking I/O requests
 * This class wraps the aws_event_loop_group from aws-c-io
 *
 * @param maxThreads If maxThreads == 0, then the loop count will be the number of available processors on the machine.
 * Otherwise, maxThreads will be the number of event loops in the group.
 * @throws [software.amazon.awssdk.kotlin.crt.CrtRuntimeException] If the system is unable to allocate space for a native event loop group
 */
@OptIn(ExperimentalUnsignedTypes::class)
public actual class EventLoopGroup actual constructor(maxThreads: Int) : CrtResource<aws_event_loop_group>(), Closeable, AsyncShutdown {
    private val elg: CPointer<aws_event_loop_group>
    private val shutdownComplete: ShutdownChannel = shutdownChannel().freeze()
    private val stableRef = StableRef.create(shutdownComplete)

    init {
        val shutdownOpts = cValue<aws_shutdown_callback_options> {
            shutdown_callback_fn = staticCFunction(::onShutdownComplete)
            shutdown_callback_user_data = stableRef.asCPointer()
        }

        elg = aws_event_loop_group_new_default(Allocator.Default, maxThreads.toUShort(), shutdownOpts) ?: throw CrtRuntimeException("aws_event_loop_group_new_default() failed")
    }

    override val ptr: CPointer<aws_event_loop_group>
        get() = elg

    /**
     * Close this ELG
     */
    override fun close() {
        aws_event_loop_group_release(elg)
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
