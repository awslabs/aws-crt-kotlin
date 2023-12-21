/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.http

import aws.sdk.kotlin.crt.io.MutableBuffer
import aws.sdk.kotlin.crt.io.byteArrayBuffer
import java.nio.ByteBuffer
import software.amazon.awssdk.crt.http.HttpHeader as HttpHeaderJni
import software.amazon.awssdk.crt.http.HttpRequest as HttpRequestJni
import software.amazon.awssdk.crt.http.HttpRequestBodyStream as HttpRequestBodyStreamJni
import software.amazon.awssdk.crt.http.HttpStream as HttpStreamJni
import software.amazon.awssdk.crt.http.HttpStreamMetrics as HttpStreamMetricsJni
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler as HttpStreamResponseHandlerJni

/**
 * Convert the KMP version of [HttpRequest] into the JNI equivalent
 */
internal fun HttpRequest.into(): HttpRequestJni {
    val jniHeaders = headers.entries()
        .map { entry ->
            entry.value.map { HttpHeaderJni(entry.key, it) }
        }
        .flatten()
        .toTypedArray()

    val bodyStream = body?.let { JniRequestBodyStream(it) }
    return HttpRequestJni(method, encodedPath, jniHeaders, bodyStream)
}

internal fun HttpStreamResponseHandler.asJniStreamResponseHandler(): HttpStreamResponseHandlerJni {
    val handler = this
    return object : HttpStreamResponseHandlerJni {
        override fun onResponseHeaders(
            stream: HttpStreamJni,
            statusCode: Int,
            blockType: Int,
            headers: Array<out HttpHeaderJni>?,
        ) {
            val ktHeaders = headers?.map { HttpHeader(it.name, it.value) }
            val ktStream = HttpStreamJVM(stream)
            handler.onResponseHeaders(ktStream, statusCode, blockType, ktHeaders)
        }

        override fun onResponseHeadersDone(stream: HttpStreamJni, blockType: Int) {
            val ktStream = HttpStreamJVM(stream)
            handler.onResponseHeadersDone(ktStream, blockType)
        }

        override fun onResponseBody(stream: HttpStreamJni, bodyBytesIn: ByteArray?): Int {
            if (bodyBytesIn == null) return 0
            val ktStream = HttpStreamJVM(stream)
            val buffer = byteArrayBuffer(bodyBytesIn)
            return handler.onResponseBody(ktStream, buffer)
        }

        override fun onResponseComplete(stream: HttpStreamJni, errorCode: Int) {
            val ktStream = HttpStreamJVM(stream)
            handler.onResponseComplete(ktStream, errorCode)
        }

        override fun onMetrics(stream: HttpStreamJni, metrics: HttpStreamMetricsJni) {
            val ktStream = HttpStreamJVM(stream)
            handler.onMetrics(ktStream, metrics.toKotlin())
        }
    }
}

/**
 * Wrapper around kotlin [HttpRequest] request body stream
 */
internal class JniRequestBodyStream(val ktStream: HttpRequestBodyStream) : HttpRequestBodyStreamJni {
    override fun sendRequestBody(bodyBytesOut: ByteBuffer?): Boolean {
        if (bodyBytesOut == null) return true
        return ktStream.sendRequestBody(MutableBuffer(bodyBytesOut))
    }

    override fun resetPosition(): Boolean = ktStream.resetPosition()
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
