/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt.http

import aws.sdk.kotlin.crt.*
import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.NativeHandle
import aws.sdk.kotlin.crt.awsAssertOpSuccess
import aws.sdk.kotlin.crt.io.Buffer
import aws.sdk.kotlin.crt.io.ByteCursorBuffer
import aws.sdk.kotlin.crt.util.asAwsByteCursor
import aws.sdk.kotlin.crt.util.initFromCursor
import aws.sdk.kotlin.crt.util.toKString
import aws.sdk.kotlin.crt.util.withAwsByteCursor
import kotlinx.cinterop.*
import libcrt.*
import platform.posix.size_t

internal class HttpClientConnectionNative(
    private val manager: HttpClientConnectionManager,
    override val ptr: CPointer<cnames.structs.aws_http_connection>,
) : Closeable, HttpClientConnection, NativeHandle<cnames.structs.aws_http_connection> {

    override val id: String = ptr.rawValue.toString()
    override fun makeRequest(httpReq: HttpRequest, handler: HttpStreamResponseHandler): HttpStream {
        val nativeReq = initRequest(httpReq)
        val cbData = HttpStreamContext(handler, nativeReq)
        val stableRef = StableRef.create(cbData)
        val reqOptions = cValue<aws_http_make_request_options> {
            self_size = sizeOf<aws_http_make_request_options>().convert()
            request = nativeReq
            user_data = stableRef.asCPointer()

            // callbacks
            on_response_headers = staticCFunction(::onResponseHeaders)
            on_response_header_block_done = staticCFunction(::onResponseHeaderBlockDone)
            on_response_body = staticCFunction(::onIncomingBody)
            on_complete = staticCFunction(::onStreamComplete)
        }

        val stream = aws_http_connection_make_request(ptr, reqOptions)

        if (stream == null) {
            aws_http_message_destroy(nativeReq)
            stableRef.dispose()
            throw CrtRuntimeException("aws_http_connection_make_request()")
        }

        return HttpStreamNative(stream)
    }

    override fun shutdown() {
        aws_http_connection_close(ptr)
    }

    override fun close() {
        manager.releaseConnection(this)
    }

    private fun initRequest(request: HttpRequest): CPointer<cnames.structs.aws_http_message> {
        val nativeReq = checkNotNull(
            aws_http_message_new_request(Allocator.Default),
        ) { "aws_http_message_new_request()" }

        try {
            awsAssertOpSuccess(
                withAwsByteCursor(request.method) { method ->
                    aws_http_message_set_request_method(nativeReq, method)
                },
            ) { "aws_http_message_set_request_method()" }

            awsAssertOpSuccess(
                withAwsByteCursor(request.encodedPath) { encodedPath ->
                    aws_http_message_set_request_path(nativeReq, encodedPath)
                },
            ) { "aws_http_message_set_request_path()" }

            request.headers.forEach { key, values ->
                // instead of usual idiomatic map(), forEach()...
                // have to be a little more careful here as some of these are temporaries and we need
                // stable memory addresses
                key.encodeToByteArray().usePinned { keyBytes ->
                    val keyCursor = keyBytes.asAwsByteCursor()
                    values.forEach {
                        it.encodeToByteArray().usePinned { valueBytes ->
                            val valueCursor = valueBytes.asAwsByteCursor()

                            val header = cValue<aws_http_header> {
                                name.initFromCursor(keyCursor)
                                value.initFromCursor(valueCursor)
                            }

                            awsAssertOpSuccess(
                                aws_http_message_add_header(nativeReq, header),
                            ) {
                                "aws_http_message_add_header()"
                            }
                        }
                    }
                }
            }

            val bodyStream = request.body?.let { inputStream(it) }
            aws_http_message_set_body_stream(nativeReq, bodyStream)
        } catch (ex: Exception) {
            aws_http_message_destroy(nativeReq)
            throw ex
        }

        return nativeReq
    }
}

/**
 * Userdata passed through the native callbacks for HTTP responses
 */
private class HttpStreamContext(
    /**
     * The actual Kotlin handler for each callback
     */
    val handler: HttpStreamResponseHandler,

    /**
     * The aws-c-http request instance
     */
    val nativeReq: CPointer<cnames.structs.aws_http_message>,
)

private fun onResponseHeaders(
    nativeStream: CPointer<cnames.structs.aws_http_stream>?,
    blockType: aws_http_header_block,
    headerArray: CPointer<aws_http_header>?,
    numHeaders: size_t,
    userdata: COpaquePointer?,
): Int {
    val ctx = userdata?.asStableRef<HttpStreamContext>()?.get() ?: return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())
    val stream = nativeStream?.let { HttpStreamNative(it) } ?: return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())

    val hdrCnt = numHeaders.toInt()
    val headers: List<HttpHeader>? = if (hdrCnt > 0 && headerArray != null) {
        val kheaders = mutableListOf<HttpHeader>()
        for (i in 0 until hdrCnt) {
            val nativeHdr = headerArray[i]
            val hdr = HttpHeader(nativeHdr.name.toKString(), nativeHdr.value.toKString())
            kheaders.add(hdr)
        }
        kheaders
    } else {
        null
    }

    try {
        ctx.handler.onResponseHeaders(stream, stream.responseStatusCode, blockType.value.toInt(), headers)
    } catch (ex: Exception) {
        return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())
    }
    return AWS_OP_SUCCESS
}

private fun onResponseHeaderBlockDone(
    nativeStream: CPointer<cnames.structs.aws_http_stream>?,
    blockType: aws_http_header_block,
    userdata: COpaquePointer?,
): Int {
    val ctx = userdata?.asStableRef<HttpStreamContext>()?.get() ?: return AWS_OP_ERR
    val stream = nativeStream?.let { HttpStreamNative(it) } ?: return AWS_OP_ERR
    try {
        ctx.handler.onResponseHeadersDone(stream, blockType.value.toInt())
    } catch (ex: Exception) {
        return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())
    }

    return AWS_OP_SUCCESS
}

private fun onIncomingBody(
    nativeStream: CPointer<cnames.structs.aws_http_stream>?,
    data: CPointer<aws_byte_cursor>?,
    userdata: COpaquePointer?,
): Int {
    val ctx = userdata?.asStableRef<HttpStreamContext>()?.get() ?: return AWS_OP_ERR
    val stream = nativeStream?.let { HttpStreamNative(it) } ?: return AWS_OP_ERR

    try {
        val body = if (data != null) ByteCursorBuffer(data) else Buffer.Empty
        val windowIncrement = ctx.handler.onResponseBody(stream, body)
        if (windowIncrement < 0) {
            return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())
        }

        if (windowIncrement > 0) {
            aws_http_stream_update_window(nativeStream, windowIncrement.convert())
        }
    } catch (ex: Exception) {
        // FIXME - we need our own logging block so we can log exceptions and errors from FFI layer
        return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())
    }

    return AWS_OP_SUCCESS
}

private fun onStreamComplete(
    nativeStream: CPointer<cnames.structs.aws_http_stream>?,
    errorCode: Int,
    userdata: COpaquePointer?,
) {
    val stableRef = userdata?.asStableRef<HttpStreamContext>() ?: return
    val ctx = stableRef.get()
    val stream = nativeStream?.let { HttpStreamNative(it) } ?: return
    try {
        ctx.handler.onResponseComplete(stream, errorCode)
    } catch (ex: Exception) {
        // close connection if callback throws an exception
        aws_http_connection_close(aws_http_stream_get_connection(nativeStream))
    } finally {
        // cleanup stream resources
        stableRef.dispose()
        aws_http_message_destroy(ctx.nativeReq)
    }
}
