/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt.http

import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.LogLevel
import aws.sdk.kotlin.crt.io.MutableBuffer
import aws.sdk.kotlin.crt.log
import kotlinx.cinterop.*
import libcrt.*

private fun streamSeek(
    stream: CPointer<aws_input_stream>?,
    offset: aws_off_t,
    basis: aws_stream_seek_basis,
): Int {
    if (stream == null || basis != AWS_SSB_BEGIN || offset != 0L) return AWS_OP_ERR
    val handler = stream.pointed.impl?.asStableRef<RequestBodyStream>()?.get() ?: return AWS_OP_ERR
    var result = AWS_OP_SUCCESS

    try {
        if (!handler.resetPosition()) {
            result = AWS_OP_ERR
        }
    } catch (ex: Exception) {
        log(LogLevel.Error, "streamSeek: $ex")
        return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())
    }

    if (result == AWS_OP_SUCCESS) {
        handler.bodyDone = false
    }
    return result
}

private fun streamRead(
    stream: CPointer<aws_input_stream>?,
    dest: CPointer<aws_byte_buf>?,
): Int {
    if (stream == null || dest == null) return AWS_OP_ERR
    val handler = stream.pointed.impl?.asStableRef<RequestBodyStream>()?.get() ?: return AWS_OP_ERR
    if (handler.bodyDone) return AWS_OP_SUCCESS

    try {
        // MutableBuffer handles updating dest->len
        val buffer = MutableBuffer(dest)
        if (handler.khandler.sendRequestBody(buffer)) {
            handler.bodyDone = true
        }
    } catch (ex: Exception) {
        log(LogLevel.Error, "streamRead: $ex")
        return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())
    }

    return AWS_OP_SUCCESS
}

private fun streamGetStatus(
    stream: CPointer<aws_input_stream>?,
    status: CPointer<aws_stream_status>?,
): Int {
    if (stream == null || status == null) return AWS_OP_ERR
    val handler = stream.pointed.impl?.asStableRef<RequestBodyStream>()?.get() ?: return AWS_OP_ERR
    status.pointed.is_end_of_stream = handler.bodyDone
    status.pointed.is_valid = true
    return AWS_OP_SUCCESS
}

@Suppress("unused")
private fun streamGetLength(
    stream: CPointer<aws_input_stream>?,
    outLength: CPointer<platform.posix.int64_tVar>?,
): Int = AWS_OP_ERR

private fun streamAcquire(
    stream: CPointer<aws_input_stream>?,
) {
    if (stream == null) return
    aws_ref_count_acquire(stream.pointed.ref_count.ptr)
}

private fun streamRelease(
    stream: CPointer<aws_input_stream>?,
) {
    if (stream == null) return
    val refCnt = aws_ref_count_release(stream.pointed.ref_count.ptr)
    if (refCnt.toInt() == 0) {
        log(LogLevel.Trace, "releasing RequestBodyStream")
        val stableRef = stream.pointed.impl?.asStableRef<RequestBodyStream>() ?: return
        stableRef.dispose()
        Allocator.Default.free(stream)
    }
}

private val requestStreamVtable: CPointer<aws_input_stream_vtable> by lazy {
    val vtable = Allocator.Default.alloc<aws_input_stream_vtable>()
    vtable.seek = staticCFunction(::streamSeek)
    vtable.read = staticCFunction(::streamRead)
    vtable.get_status = staticCFunction(::streamGetStatus)
    vtable.get_length = staticCFunction(::streamGetLength)
    vtable.acquire = staticCFunction(::streamAcquire)
    vtable.release = staticCFunction(::streamRelease)
    vtable.ptr
}

/**
 * Create an aws_input_stream instance for the HTTP request body
 */
internal fun inputStream(khandler: HttpRequestBodyStream): CPointer<aws_input_stream> {
    val stream: aws_input_stream = Allocator.Default.alloc()
    stream.vtable = requestStreamVtable
    val impl = RequestBodyStream(khandler)
    val stableRef = StableRef.create(impl)
    stream.impl = stableRef.asCPointer()
    return stream.ptr
}

internal fun CPointer<aws_input_stream>.toHttpRequestBodyStream(): HttpRequestBodyStream {
    val stableRef = checkNotNull(this.pointed.impl?.asStableRef<RequestBodyStream>()) { "toHttpRequestBodyStream() expected non-null `impl`" }
    return stableRef.get().khandler
}

// wrapper around the actual implementation
private class RequestBodyStream(
    val khandler: HttpRequestBodyStream,
    var bodyDone: Boolean = false,
) : HttpRequestBodyStream by khandler