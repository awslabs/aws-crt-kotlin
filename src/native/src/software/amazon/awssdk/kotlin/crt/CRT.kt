/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.toKString
import libcommon.*

actual object CRT {
    /**
     * Returns the last error on the current thread.
     * @return Last error code recorded in this thread
     */
    actual fun awsLastError(): Int {
        return aws_last_error()
    }

    /**
     * Given an integer error code from an internal operation
     * @param errorCode An error code returned from an exception or other native function call
     * @return A user-friendly description of the error
     */
    actual fun awsErrorString(errorCode: Int): String? {
        return aws_error_str(errorCode)?.toKString()
    }

    /**
     * Given an integer error code from an internal operation
     *
     * @param errorCode An error code returned from an exception or other native
     * function call
     * @return A string identifier for the error
     */
    actual fun awsErrorName(errorCode: Int): String? {
        return aws_error_name(errorCode)?.toKString()
    }

    /**
     * @return The number of bytes allocated in native resources. If aws.crt.memory.tracing is 1 or 2, this will
     * be a non-zero value. Otherwise, no tracing will be done, and the value will always be 0
     */
    actual fun nativeMemory(): Long {
        TODO("Not yet implemented")
    }

}
