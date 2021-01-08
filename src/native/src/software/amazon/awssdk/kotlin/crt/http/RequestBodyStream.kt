/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import kotlinx.cinterop.*
import libcrt.*
import software.amazon.awssdk.kotlin.crt.Allocator
import software.amazon.awssdk.kotlin.crt.io.AwsByteBuf
import kotlin.native.concurrent.atomicLazy

@OptIn(ExperimentalUnsignedTypes::class)
private fun streamSeek(
    stream: CPointer<aws_input_stream>?,
    offset: aws_off_t,
    basis: aws_stream_seek_basis
): Int {
    initRuntimeIfNeeded()
    if (stream == null || basis != AWS_SSB_BEGIN || offset != 0L) return AWS_OP_ERR
    val handler = stream.pointed.impl?.asStableRef<RequestBodyStream>()?.get() ?: return AWS_OP_ERR
    var result = AWS_OP_SUCCESS

    try {
        if (!handler.resetPosition()) {
            result = AWS_OP_ERR
        }
    } catch (ex: Exception) {
        return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())
    }

    if (result == AWS_OP_SUCCESS) {
        handler.bodyDone = false
    }
    return result
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun streamRead(
    stream: CPointer<aws_input_stream>?,
    dest: CPointer<aws_byte_buf>?
): Int {
    initRuntimeIfNeeded()
    if (stream == null || dest == null) return AWS_OP_ERR
    val handler = stream.pointed.impl?.asStableRef<RequestBodyStream>()?.get() ?: return AWS_OP_ERR
    if (handler.bodyDone) return AWS_OP_SUCCESS

    try {
        // AwsByteBuf handles updating the dest->len
        val buffer = AwsByteBuf(dest)
        handler.khandler.sendRequestBody(buffer)
    } catch (ex: Exception) {
        return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())
    }

    return AWS_OP_SUCCESS
}

private fun streamGetStatus(
    stream: CPointer<aws_input_stream>?,
    status: CPointer<aws_stream_status>?
): Int {
    initRuntimeIfNeeded()
    if (stream == null || status == null) return AWS_OP_ERR
    val handler = stream.pointed.impl?.asStableRef<RequestBodyStream>()?.get() ?: return AWS_OP_ERR
    status.pointed.is_end_of_stream = handler.bodyDone
    status.pointed.is_valid = true
    return AWS_OP_SUCCESS
}

private fun streamGetLength(
    stream: CPointer<aws_input_stream>?,
    outLength: CPointer<platform.posix.int64_tVar>?
): Int {
    return AWS_OP_ERR
}

private fun streamDestroy(
    stream: CPointer<aws_input_stream>?,
) {
    if (stream == null) return
    val stableRef = stream.pointed.impl?.asStableRef<RequestBodyStream>() ?: return
    stableRef.dispose()
    Allocator.Default.free(stream)
}

// FIXME - haven't figured out how to get CPointer<T> statically
@SharedImmutable
private val requestStreamVtable: CPointer<aws_input_stream_vtable> by atomicLazy {
    val vtable = Allocator.Default.alloc<aws_input_stream_vtable>()
    vtable.seek = staticCFunction(::streamSeek)
    vtable.read = staticCFunction(::streamRead)
    vtable.get_status = staticCFunction(::streamGetStatus)
    vtable.get_length = staticCFunction(::streamGetLength)
    vtable.destroy = staticCFunction(::streamDestroy)

    vtable.ptr
}

/**
 * Create an aws_input_stream instance for the HTTP request body
 */
internal fun inputStream(khandler: HttpRequestBodyStream): CPointer<aws_input_stream> {
    val stream: aws_input_stream = Allocator.Default.alloc()
    stream.allocator = Allocator.Default.allocator
    stream.vtable = requestStreamVtable
    val impl = RequestBodyStream(khandler)
    val stableRef = StableRef.create(impl)
    stream.impl = stableRef.asCPointer()
    return stream.ptr
}

// wrapper around the actual implementation
private class RequestBodyStream(
    val khandler: HttpRequestBodyStream,
    var bodyDone: Boolean = false
) : HttpRequestBodyStream by khandler
