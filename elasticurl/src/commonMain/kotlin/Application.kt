/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import software.amazon.awssdk.kotlin.crt.CRT
import software.amazon.awssdk.kotlin.crt.http.*
import software.amazon.awssdk.kotlin.crt.io.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val DEFAULT_CONNECT_TIMEOUT_MS = 3000

fun main(args: Array<String>) {
    CRT.initRuntime()

    val opts = CliOpts.from(args)

    println("url: ${opts.url}")
    val uri = Uri.parse(opts.url)

    // TODO - rename type
    val tlsContextBuilder = TlsContextBuilder()

    if (uri.scheme.requiresTls()) {
        // TODO - mutual tls, process cacert and key

        tlsContextBuilder.caDir = opts.capath
        tlsContextBuilder.caRoot = opts.cacert
        tlsContextBuilder.verifyPeer = !opts.insecure
    } else {
        if (opts.requireHttp2) {
            throw IllegalArgumentException("error, we don't support h2c, please use TLS for HTTP/2 connection")
        }
    }

    val tlsContext = TlsContext(tlsContextBuilder.build())

    val socketOpts = SocketOptions(connectTimeoutMs = opts.connectTimeout ?: DEFAULT_CONNECT_TIMEOUT_MS)
    val elg = EventLoopGroup()
    val hr = HostResolver(elg)

    val clientBootstrap = ClientBootstrap(elg, hr)

    val httpConnOptions = HttpClientConnectionManagerOptions.build {
        this.tlsContext = tlsContext
        this.uri = uri
        this.clientBootstrap = clientBootstrap
        socketOptions = socketOpts
    }

    val httpConnManager = HttpClientConnectionManager(httpConnOptions)

    runBlocking {
        val conn = httpConnManager.acquireConnection()

        val request = HttpRequest.build {
            method = opts.httpMethod.name
            opts.headers?.map(::headerPair)?.forEach { headers.append(it.first, it.second) }
            encodedPath = uri.path
        }

        val streamDone = Channel<Unit>()
        val responseHandler = object : HttpStreamResponseHandler {
            override fun onResponseHeaders(
                stream: HttpStream,
                responseStatusCode: Int,
                blockType: Int,
                nextHeaders: List<HttpHeader>?
            ) {
                println("onResponseHeaders -- status: $responseStatusCode ")
                println("headers:")
                nextHeaders?.forEach { println("\t${it.name}: ${it.value}") }
            }

            override fun onResponseComplete(stream: HttpStream, errorCode: Int) {
                println("onResponseComplete: errorCode: $errorCode")
                streamDone.offer(Unit)
            }
        }

        val stream = conn.makeRequest(request, responseHandler)
        stream.activate()

        streamDone.receive()
    }
}

private fun headerPair(raw: String): Pair<String, String> {
    val parts = raw.split(":", limit = 2)
    require(parts.size == 2) { "invalid HTTP header specified: $raw " }
    return parts[0] to parts[1]
}

/**
 * MPP compatible runBlocking to run suspend functions from common
 */
internal expect fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T
