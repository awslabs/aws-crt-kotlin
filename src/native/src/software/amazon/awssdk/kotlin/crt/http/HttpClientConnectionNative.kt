/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import kotlinx.cinterop.*
import libcrt.*
import platform.posix.size_t
import software.amazon.awssdk.kotlin.crt.*
import software.amazon.awssdk.kotlin.crt.io.Buffer
import software.amazon.awssdk.kotlin.crt.io.ByteCursorBuffer

// TODO - port over tests from crt-java

internal class HttpClientConnectionNative(
    private val manager: HttpClientConnectionManager,
    private val connection: CPointer<aws_http_connection>
) : HttpClientConnection, Closeable, CrtResource<aws_http_connection>() {

    override val ptr: CPointer<aws_http_connection> = connection

    override fun makeRequest(httpReq: HttpRequest, handler: HttpStreamResponseHandler): HttpStream {
        val nativeRequest = initRequest(httpReq)
        val cbData = StreamContext(handler, nativeRequest)
        val stableRef = StableRef.create(cbData)
        cbData.stableRef = stableRef

        val reqOptions = cValue<aws_http_make_request_options> {
            self_size = sizeOf<aws_http_make_request_options>().convert()
            request = nativeRequest
            user_data = stableRef.asCPointer()

            // callbacks
            on_response_headers = staticCFunction(::onResponseHeaders)
            on_response_header_block_done = staticCFunction(::onResponseHeaderBlockDone)
            on_response_body = staticCFunction(::onIncomingBody)
            on_complete = staticCFunction(::onStreamComplete)
        }

        val stream = awsAssertNotNull(
            aws_http_connection_make_request(connection, reqOptions)
        ) {
            aws_http_message_destroy(nativeRequest)
            stableRef.dispose()
            "unable to execute request"
        }

        return HttpStreamNative(stream)
    }

    private fun initRequest(request: HttpRequest): CPointer<aws_http_message> {
        val nativeReq = awsAssertNotNull(
            aws_http_message_new_request(Allocator.Default)
        ) { "aws_http_message_new_request()" }

        try {
            awsAssertOp(
                withAwsByteCursor(request.method) { method ->
                    aws_http_message_set_request_method(nativeReq, method)
                }
            ) { "aws_http_message_set_request_method()" }

            awsAssertOp(
                withAwsByteCursor(request.encodedPath) { encodedPath ->
                    aws_http_message_set_request_path(nativeReq, encodedPath)
                }
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

                            awsAssertOp(
                                aws_http_message_add_header(nativeReq, header)
                            ) {
                                "aws_http_message_add_header()"
                            }
                        }
                    }
                }
            }

            // TODO - native body stream
            // if (request.body != null) {
            //     aws_http_message_set_body_stream(nativeReq, bodyStream)
            // }
        } catch (ex: Exception) {
            aws_http_message_destroy(nativeReq)
            throw ex
        }

        return nativeReq
    }

    override suspend fun close() {
        TODO("Not yet implemented")
    }
}

/**
 * Userdata passed through the native callbacks for HTTP responses
 */
private class StreamContext(
    /**
     * The actual Kotlin handler for each callback
     */
    val handler: HttpStreamResponseHandler,

    /**
     * The aws-c-http request instance
     */
    val nativeReq: CPointer<aws_http_message>
) {
    // the ref is stashed away after creating the context and is disposed of after the stream on_complete callback runs
    var stableRef: StableRef<StreamContext>? = null
}

// See https://kotlinlang.org/docs/reference/native/c_interop.html#callbacks

@OptIn(ExperimentalUnsignedTypes::class)
private fun onResponseHeaders(
    nativeStream: CPointer<aws_http_stream>?,
    blockType: aws_http_header_block,
    headerArray: CPointer<aws_http_header>?,
    numHeaders: size_t,
    userdata: COpaquePointer?
): Int {
    initRuntimeIfNeeded()
    val ctx = userdata?.asStableRef<StreamContext>()?.get() ?: return AWS_OP_ERR
    val stream = nativeStream?.let { HttpStreamNative(it) } ?: return AWS_OP_ERR

    val hdrCnt = numHeaders.toInt()
    val headers: List<HttpHeader>? = if (hdrCnt > 0 && headerArray != null) {
        val kheaders: MutableList<HttpHeader> = ArrayList(hdrCnt)
        for (i in 0..hdrCnt) {
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

@OptIn(ExperimentalUnsignedTypes::class)
private fun onResponseHeaderBlockDone(
    nativeStream: CPointer<aws_http_stream>?,
    blockType: aws_http_header_block,
    userdata: COpaquePointer?
): Int {
    initRuntimeIfNeeded()

    val ctx = userdata?.asStableRef<StreamContext>()?.get() ?: return AWS_OP_ERR
    val stream = nativeStream?.let { HttpStreamNative(it) } ?: return AWS_OP_ERR
    try {
        ctx.handler.onResponseHeadersDone(stream, blockType.value.toInt())
    } catch (ex: Exception) {
        return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())
    }

    return AWS_OP_SUCCESS
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun onIncomingBody(
    nativeStream: CPointer<aws_http_stream>?,
    data: CPointer<aws_byte_cursor>?,
    userdata: COpaquePointer?
): Int {
    initRuntimeIfNeeded()

    val ctx = userdata?.asStableRef<StreamContext>()?.get() ?: return AWS_OP_ERR
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
        return aws_raise_error(AWS_ERROR_HTTP_CALLBACK_FAILURE.toInt())
    }

    return AWS_OP_SUCCESS
}

private fun onStreamComplete(
    nativeStream: CPointer<aws_http_stream>?,
    errorCode: Int,
    userdata: COpaquePointer?
) {
    initRuntimeIfNeeded()
    val ctx = userdata?.asStableRef<StreamContext>()?.get() ?: return
    val stream = nativeStream?.let { HttpStreamNative(it) } ?: return
    try {
        ctx.handler.onResponseComplete(stream, errorCode)
    } catch (ex: Exception) {
        // close connection if callback throws an exception
        aws_http_connection_close(aws_http_stream_get_connection(nativeStream))
    } finally {
        // cleanup stream resources
        val stableRef = ctx.stableRef
        ctx.stableRef = null
        stableRef?.dispose()
        aws_http_message_destroy(ctx.nativeReq)
    }
}
