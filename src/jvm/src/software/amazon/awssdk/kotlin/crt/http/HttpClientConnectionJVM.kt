/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import software.amazon.awssdk.kotlin.crt.io.MutableBuffer
import software.amazon.awssdk.kotlin.crt.io.byteArrayBuffer
import java.nio.ByteBuffer
import software.amazon.awssdk.crt.http.HttpClientConnection as HttpClientConnectionJni
import software.amazon.awssdk.crt.http.HttpHeader as HttpHeaderJni
import software.amazon.awssdk.crt.http.HttpRequest as HttpClientRequestJni
import software.amazon.awssdk.crt.http.HttpRequestBodyStream as HttpRequestBodyStreamJni
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler as HttpStreamResponseHandlerJni

/**
 * Wrapper around JNI HttpClientConnection type that implements the expected KMP interface
 */
internal class HttpClientConnectionJVM constructor(internal val jniConn: HttpClientConnectionJni) : HttpClientConnection {

    override fun makeRequest(httpReq: HttpRequest, handler: HttpStreamResponseHandler): HttpStream {
        val jniStream = jniConn.makeRequest(httpReq.into(), handler.asJniStreamResponseHandler())
        return HttpStreamJVM(jniStream)
    }

    override suspend fun close() {
        jniConn.close()
    }
}

private fun HttpRequest.into(): HttpClientRequestJni {
    val jniHeaders = headers.entries()
        .map { entry ->
            entry.value.map {
                HttpHeaderJni(entry.key, it)
            }
        }
        .flatten()
        .toTypedArray()

    val bodyStream = body?.asJniRequestBodyStream()
    return HttpClientRequestJni(method, encodedPath, jniHeaders, bodyStream)
}

private fun HttpStreamResponseHandler.asJniStreamResponseHandler(): HttpStreamResponseHandlerJni {
    val handler = this
    return object : HttpStreamResponseHandlerJni {
        override fun onResponseHeaders(
            stream: software.amazon.awssdk.crt.http.HttpStream,
            statusCode: Int,
            blockType: Int,
            headers: Array<out software.amazon.awssdk.crt.http.HttpHeader>?
        ) {
            val ktHeaders = headers?.map { HttpHeader(it.name, it.value) }
            val ktStream = HttpStreamJVM(stream)
            handler.onResponseHeaders(ktStream, statusCode, blockType, ktHeaders)
        }

        override fun onResponseHeadersDone(stream: software.amazon.awssdk.crt.http.HttpStream, blockType: Int) {
            val ktStream = HttpStreamJVM(stream)
            handler.onResponseHeadersDone(ktStream, blockType)
        }

        override fun onResponseBody(stream: software.amazon.awssdk.crt.http.HttpStream, bodyBytesIn: ByteArray?): Int {
            if (bodyBytesIn == null) return 0
            val ktStream = HttpStreamJVM(stream)
            val buffer = byteArrayBuffer(bodyBytesIn)
            return handler.onResponseBody(ktStream, buffer)
        }

        override fun onResponseComplete(stream: software.amazon.awssdk.crt.http.HttpStream, errorCode: Int) {
            val ktStream = HttpStreamJVM(stream)
            handler.onResponseComplete(ktStream, errorCode)
        }
    }
}

private fun HttpRequestBodyStream.asJniRequestBodyStream(): HttpRequestBodyStreamJni {
    val ktStream = this
    return object : HttpRequestBodyStreamJni {
        override fun sendRequestBody(bodyBytesOut: ByteBuffer?): Boolean {
            if (bodyBytesOut == null) return true
            return ktStream.sendRequestBody(ByteBufferMutableBuffer(bodyBytesOut))
        }

        override fun resetPosition(): Boolean {
            return ktStream.resetPosition()
        }
    }
}

/**
 * Wrap a [ByteBuffer] instance as a [MutableBuffer]
 */
private class ByteBufferMutableBuffer(val buf: ByteBuffer) : MutableBuffer {
    override val capacity: Long
        get() = buf.capacity().toLong()

    override val len: Int
        get() = buf.position()

    override fun write(src: ByteArray): Int {
        buf.put(src)
        return src.size
    }

    override fun copyTo(dest: ByteArray, offset: Int): Int {
        val size = minOf(dest.size, len)
        buf.get(dest, offset, size)
        return size
    }

    override fun readAll(): ByteArray {
        val bytes = ByteArray(buf.position())
        buf.get(bytes)
        return bytes
    }
}
