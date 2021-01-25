/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.io

import kotlinx.coroutines.future.await
import aws.sdk.kotlin.crt.AsyncShutdown
import aws.sdk.kotlin.crt.Closeable
import software.amazon.awssdk.crt.io.EventLoopGroup as EventLoopGroupJni

/**
 * Creates a new event loop group for the I/O subsystem to use to run blocking I/O requests
 * This class wraps the aws_event_loop_group from aws-c-io
 *
 * @param maxThreads If maxThreads == 0, then the loop count will be the number of available processors on the machine.
 * Otherwise, maxThreads will be the number of event loops in the group.
 * @throws [aws.sdk.kotlin.crt.CrtRuntimeException] If the system is unable to allocate space for a native event loop group
 */
public actual class EventLoopGroup actual constructor(maxThreads: Int) : Closeable, aws.sdk.kotlin.crt.AsyncShutdown {
    internal val jniElg = EventLoopGroupJni(maxThreads)

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
