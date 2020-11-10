/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import software.amazon.awssdk.kotlin.crt.CrtTest
import software.amazon.awssdk.kotlin.crt.io.*
import software.amazon.awssdk.kotlin.crt.use
import kotlin.test.assertNotNull

const val DEFAULT_CONNECT_TIMEOUT_MS: Int = 30_000

/**
 * (WIP) re-usable test fixture for HTTP request/response testing.
 */
abstract class HttpClientTest : CrtTest() {
    /**
     * Run the given block with a ClientBootstrap instance
     */
    suspend inline fun <reified T> withDefaultBootstrap(block: (ClientBootstrap) -> T): T {
        val elg = EventLoopGroup()
        val hr = HostResolver(elg)
        val clientBootstrap = ClientBootstrap(elg, hr)
        try {
            return block(clientBootstrap)
        } finally {
            clientBootstrap.close()
            hr.close()
            elg.close()
        }
    }

    /**
     * Connects to the given URL. On success no exception should occur
     * @throws TimeoutCancellationException if the connection fails to connect in the time allotted by [connTimeoutMs]
     */
    suspend fun connect(
        url: String,
        clientBootstrap: ClientBootstrap,
        tlsContext: TlsContext,
        socketOptions: SocketOptions? = null,
        connTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
        block: suspend (HttpClientConnection) -> Unit = {}
    ) {
        val uri = Uri.parse(url)
        val httpConnOpts = HttpClientConnectionManagerOptions.build {
            this.uri = uri
            this.clientBootstrap = clientBootstrap
            this.tlsContext = tlsContext
            this.socketOptions = socketOptions ?: SocketOptions()
        }
        HttpClientConnectionManager(httpConnOpts).use { pool ->
            withTimeout(connTimeoutMs.toLong()) {
                pool.acquireConnection().use {
                    block(it)
                }
            }
        }
    }

    /**
     * Simplified request/response handling. Round trip an http request to the given [url] with the
     * provided http [verb] and optional [body] as a string.
     */
    suspend fun roundTrip(url: String, verb: String = "GET", body: String? = null): HttpTestResponse {
        val responseHandler = HttpTestResponseHandler()
        var response: HttpTestResponse? = null
        withDefaultBootstrap { bootstrap ->
            TlsContext().use { tlsContext ->
                connect(url, bootstrap, tlsContext) { conn ->
                    val uri = Uri.parse(url)
                    val bodyBytes = body?.encodeToByteArray()

                    val request = HttpRequest.build {
                        method = verb
                        encodedPath = uri.path
                        headers.append("Host", uri.host)
                        if (bodyBytes != null) {
                            headers.append("Content-Length", bodyBytes.size.toString())
                            this.body = HttpTestRequestBody(bodyBytes)
                        }
                    }

                    val stream = conn.makeRequest(request, responseHandler)
                    try {
                        stream.activate()
                        response = responseHandler.waitForResponse()
                    } finally {
                        stream.close()
                    }
                }
            }
        }

        return assertNotNull(response, "[$url]: http response was never set")
    }
}

data class HttpTestResponse(val statusCode: Int, val headers: Headers, val body: ByteArray? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HttpTestResponse

        if (statusCode != other.statusCode) return false
        if (headers != other.headers) return false
        if (body != null) {
            if (other.body == null) return false
            if (!body.contentEquals(other.body)) return false
        } else if (other.body != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + headers.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }
}

private class HttpTestRequestBody(private val bytes: ByteArray) : HttpRequestBodyStream {
    private var currPos: Int = 0

    override fun sendRequestBody(buffer: MutableBuffer): Boolean {
        // inefficient copy...
        val outgoing = bytes.sliceArray(currPos..bytes.size)
        currPos += buffer.write(outgoing)
        return currPos == bytes.size
    }

    override fun resetPosition(): Boolean {
        currPos = 0
        return true
    }
}

private class HttpTestResponseHandler : HttpStreamResponseHandler {
    private val streamDone = Channel<Int>(1)
    private val statusCode: AtomicInt = atomic(0)

    override fun onResponseHeaders(
        stream: HttpStream,
        responseStatusCode: Int,
        blockType: Int,
        nextHeaders: List<HttpHeader>?
    ) {
        // FIXME - getting the headers or body out of these callbacks in k/n is...difficult
        // k/n concurrency rules causes an immutability exception - we'd have to use something like DetachedObjectGraph to update the values
        statusCode.compareAndSet(0, responseStatusCode)
    }

    override fun onResponseBody(stream: HttpStream, bodyBytesIn: Buffer): Int {
        return super.onResponseBody(stream, bodyBytesIn)
    }

    override fun onResponseComplete(stream: HttpStream, errorCode: Int) {
        streamDone.offer(errorCode)
        streamDone.close()
    }

    suspend fun waitForResponse(): HttpTestResponse {
        val errorCode = streamDone.receive()
        if (errorCode != 0) {
            throw HttpException(errorCode)
        }

        val headers = HeadersBuilder()
        return HttpTestResponse(statusCode.value, headers.build(), null)
    }
}