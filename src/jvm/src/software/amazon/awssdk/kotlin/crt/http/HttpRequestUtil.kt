/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import software.amazon.awssdk.kotlin.crt.io.MutableBuffer
import software.amazon.awssdk.kotlin.crt.io.byteArrayBuffer
import java.nio.ByteBuffer
import software.amazon.awssdk.crt.http.HttpRequest as HttpRequestJni
import software.amazon.awssdk.crt.http.HttpRequestBodyStream as HttpRequestBodyStreamJni

/**
 * Convert the KMP version of [HttpRequest] into the JNI equivalent
 */
internal fun HttpRequest.into(): HttpRequestJni {
    val jniHeaders = headers.entries()
        .map { entry ->
            entry.value.map {
                software.amazon.awssdk.crt.http.HttpHeader(entry.key, it)
            }
        }
        .flatten()
        .toTypedArray()

    val bodyStream = body?.let { JniRequestBodyStream(it) }
    return software.amazon.awssdk.crt.http.HttpRequest(method, encodedPath, jniHeaders, bodyStream)
}

internal fun HttpStreamResponseHandler.asJniStreamResponseHandler(): software.amazon.awssdk.crt.http.HttpStreamResponseHandler {
    val handler = this
    return object : software.amazon.awssdk.crt.http.HttpStreamResponseHandler {
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

/**
 * Wrapper around kotlin [HttpRequest] request body stream
 */
internal class JniRequestBodyStream(val ktStream: HttpRequestBodyStream) : HttpRequestBodyStreamJni {
    override fun sendRequestBody(bodyBytesOut: ByteBuffer?): Boolean {
        if (bodyBytesOut == null) return true
        return ktStream.sendRequestBody(ByteBufferMutableBuffer(bodyBytesOut))
    }

    override fun resetPosition(): Boolean {
        return ktStream.resetPosition()
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

/**
 * Convert a JNI HttpRequest back to our KMP version
 */
internal fun HttpRequest.Companion.from(jniRequest: HttpRequestJni): HttpRequest = build {
    method = jniRequest.method
    encodedPath = jniRequest.encodedPath
    headers {
        jniRequest.headers.forEach {
            append(it.name, it.value)
        }
    }

    val jniBodyStream = jniRequest.bodyStream
    if (jniBodyStream != null) {
        if (jniBodyStream is JniRequestBodyStream) {
            body = jniBodyStream.ktStream
        } else {
            // need to fill in support to proxy via an (possibly anonymous) object that implements HttpRequestBodyStream
            TODO("JNI request body stream is not an instance of JniRequestBodyStream - proxying other stream types not implemented yet")
        }
    }
}
