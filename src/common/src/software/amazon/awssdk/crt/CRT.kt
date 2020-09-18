/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.crt

expect object CRT {
    /**
     * Returns the last error on the current thread.
     * @return Last error code recorded in this thread
     */
    fun awsLastError(): Int

    /**
     * Given an integer error code from an internal operation
     * @param errorCode An error code returned from an exception or other native function call
     * @return A user-friendly description of the error
     */
    fun awsErrorString(errorCode: Int): String?

    /**
     * Given an integer error code from an internal operation
     *
     * @param errorCode An error code returned from an exception or other native
     * function call
     * @return A string identifier for the error
     */
    fun awsErrorName(errorCode: Int): String?

    /**
     * @return The number of bytes allocated in native resources. If aws.crt.memory.tracing is 1 or 2, this will
     * be a non-zero value. Otherwise, no tracing will be done, and the value will always be 0
     */
    fun nativeMemory(): Long
}
