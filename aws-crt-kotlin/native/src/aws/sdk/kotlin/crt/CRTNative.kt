/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import libcrt.*

@OptIn(ExperimentalForeignApi::class)
public actual object CRT {
    private val initialized = atomic(false)

    /**
     * Initialize the CRT libraries if needed
     */
    public actual fun initRuntime(block: Config.() -> Unit) {
        if (!initialized.compareAndSet(false, true)) return

        val config = Config().apply(block)

        // bootstrap our allocator
        s_crt_kotlin_init_allocator(CrtDebug.traceLevel)

        // aws_common_library_init(Allocator.Default)
        // aws_cal_library_init(Allocator.Default)
        // aws_cal_library_init(Allocator.Default)
        // aws_io_library_init(Allocator.Default);
        // aws_compression_library_init(Allocator.Default);
        // aws_http_library_init(Allocator.Default);
        // aws_auth_library_init(Allocator.Default);

        // TODO - init logging
        // TODO - cleanup CRT atexti
    }

    /**
     * Returns the last error on the current thread.
     * @return Last error code recorded in this thread
     */
    public actual fun lastError(): Int = aws_last_error()

    /**
     * Given an integer error code from an internal operation
     * @param errorCode An error code returned from an exception or other native function call
     * @return A user-friendly description of the error
     */
    public actual fun errorString(errorCode: Int): String? = aws_error_str(errorCode)?.toKString()

    /**
     * Given an integer error code from an internal operation
     *
     * @param errorCode An error code returned from an exception or other native
     * function call
     * @return A string identifier for the error
     */
    public actual fun errorName(errorCode: Int): String? = aws_error_name(errorCode)?.toKString()

    public actual fun isHttpErrorRetryable(errorCode: Int): Boolean =
        // see https://github.com/awslabs/aws-crt-java/blob/v0.29.10/src/native/http_request_response.c#L792
        when(errorCode.toUInt()) {
            AWS_ERROR_HTTP_HEADER_NOT_FOUND,
            AWS_ERROR_HTTP_INVALID_HEADER_FIELD,
            AWS_ERROR_HTTP_INVALID_HEADER_NAME,
            AWS_ERROR_HTTP_INVALID_HEADER_VALUE,
            AWS_ERROR_HTTP_INVALID_METHOD,
            AWS_ERROR_HTTP_INVALID_PATH,
            AWS_ERROR_HTTP_INVALID_STATUS_CODE,
            AWS_ERROR_HTTP_MISSING_BODY_STREAM,
            AWS_ERROR_HTTP_INVALID_BODY_STREAM,
            AWS_ERROR_HTTP_OUTGOING_STREAM_LENGTH_INCORRECT,
            AWS_ERROR_HTTP_CALLBACK_FAILURE,
            AWS_ERROR_HTTP_STREAM_MANAGER_SHUTTING_DOWN,
            AWS_HTTP2_ERR_CANCEL -> false
            else -> true
        }

    /**
     * @return The number of bytes allocated in native resources. If aws.crt.memory.tracing is 1 or 2, this will
     * be a non-zero value. Otherwise, no tracing will be done, and the value will always be 0
     */
    public actual fun nativeMemory(): Long =
        if (CrtDebug.traceLevel > 0) {
            // aws_mem_tracer_bytes(Allocator.Default).convert()
            TODO()
        }else {
            0
        }
}
