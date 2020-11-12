/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import kotlinx.coroutines.future.await
import software.amazon.awssdk.kotlin.crt.AsyncShutdown
import software.amazon.awssdk.kotlin.crt.Closeable
import software.amazon.awssdk.crt.io.EventLoopGroup as EventLoopGroupJni

/**
 * Creates a new event loop group for the I/O subsystem to use to run blocking I/O requests
 * This class wraps the aws_event_loop_group from aws-c-io
 *
 * @param numThreads The number of threads that the event loop group may run tasks across. Usually 1.
 * @throws [software.amazon.awssdk.kotlin.crt.CrtRuntimeException] If the system is unable to allocate space for a native event loop group
 */
public actual class EventLoopGroup actual constructor(numThreads: Int) : Closeable, AsyncShutdown {
    internal val jniElg = EventLoopGroupJni(numThreads)

    /**
     * Close this ELG
     */
    override fun close() {
        jniElg.close()
    }

    override suspend fun waitForShutdown() {
        jniElg.shutdownCompleteFuture.await()
    }
}
