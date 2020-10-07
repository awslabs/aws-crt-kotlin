/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import libcrt.*
import platform.posix.atexit
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.freeze

public actual object CRT {
    private val initialized: AtomicInt = AtomicInt(0).freeze()
    init {
        initRuntime()
    }

    public actual fun initRuntime() {
        if (!initialized.compareAndSet(0, 1)) return

        // bootstrap our allocator defined in crt.def
        s_crt_kotlin_init_allocator(CrtDebug.traceLevel)

        aws_common_library_init(Allocator.Default)
        aws_io_library_init(Allocator.Default)
        aws_compression_library_init(Allocator.Default)
        aws_http_library_init(Allocator.Default)
        Logging.initialize()

        atexit(staticCFunction(::finalCleanup))

        // fixme - yes yes I know...for now be quiet
        // Platform.isMemoryLeakCheckerActive = false
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
