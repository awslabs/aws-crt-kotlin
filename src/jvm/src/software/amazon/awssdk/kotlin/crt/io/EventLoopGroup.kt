/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

/**
 * Creates a new event loop group for the I/O subsystem to use to run blocking I/O requests
 * This class wraps the aws_event_loop_group from aws-c-io
 *
 * @param numThreads The number of threads that the event loop group may run tasks across. Usually 1.
 * @throws [software.amazon.awssdk.kotlin.crt.CrtRuntimeException] If the system is unable to allocate space for a native event loop group
 */
actual class EventLoopGroup actual constructor(numThreads: Int) {
    actual companion object {
        actual val DEFAULT: EventLoopGroup
            get() = TODO("Not yet implemented")
    }

    /**
     * Close this ELG
     */
    actual suspend fun close() {
    }
}
