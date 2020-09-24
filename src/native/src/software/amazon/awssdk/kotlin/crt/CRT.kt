/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.toKString
import libcrt.*
import kotlin.native.concurrent.AtomicInt

public actual object CRT {
    init {
        initRuntime()
    }
    private val initialized: AtomicInt = AtomicInt(0)

    public actual fun initRuntime() {
        // if (!initialized.compareAndSet(0, 1)) return
        aws_common_library_init(Allocator.Default)
        aws_io_library_init(Allocator.Default)
    }

    /**
     * Returns the last error on the current thread.
     * @return Last error code recorded in this thread
     */
    public actual fun awsLastError(): Int {
        return aws_last_error()
    }

    /**
     * Given an integer error code from an internal operation
     * @param errorCode An error code returned from an exception or other native function call
     * @return A user-friendly description of the error
     */
    public actual fun awsErrorString(errorCode: Int): String? {
        return aws_error_str(errorCode)?.toKString()
    }

    /**
     * Given an integer error code from an internal operation
     *
     * @param errorCode An error code returned from an exception or other native
     * function call
     * @return A string identifier for the error
     */
    public actual fun awsErrorName(errorCode: Int): String? {
        return aws_error_name(errorCode)?.toKString()
    }

    /**
     * @return The number of bytes allocated in native resources. If aws.crt.memory.tracing is 1 or 2, this will
     * be a non-zero value. Otherwise, no tracing will be done, and the value will always be 0
     */
    public actual fun nativeMemory(): Long {
        TODO("Not yet implemented")
    }
}
