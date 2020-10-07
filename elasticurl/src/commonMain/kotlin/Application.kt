/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import software.amazon.awssdk.kotlin.crt.CRT
import software.amazon.awssdk.kotlin.crt.CrtRuntimeException
import software.amazon.awssdk.kotlin.crt.LogDestination
import software.amazon.awssdk.kotlin.crt.http.*
import software.amazon.awssdk.kotlin.crt.io.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val DEFAULT_CONNECT_TIMEOUT_MS = 3000

fun main(args: Array<String>) {
    val opts = CliOpts.from(args)

    CRT.initRuntime() {
        logLovel = opts.logLevel
        if (opts.traceFile != null) {
            logDestination = LogDestination.File
            logFile = opts.traceFile
        }
    }

    println("url: ${opts.url}")
    val uri = Uri.parse(opts.url)
    println("parsed uri: $uri")

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

    val request = HttpRequest.build {
        method = opts.httpMethod.name
        opts.headers?.map(::headerPair)?.forEach { headers.append(it.first, it.second) }
        encodedPath = uri.path

        // have to manualy add a user-agent and host header
        headers.append("User-Agent", "elasticurl_kotlin 1.0, Powered by the AWS Common Runtime.")
        headers.append("Host", uri.host)
    }

    runBlocking {
        val conn = httpConnManager.acquireConnection()

        try {
            conn.roundTrip(request)
        } catch (ex: Exception) {
            println("failed to round trip request: ${ex.message}")
            if (ex is CrtRuntimeException) {
                println("CrtException: name: ${ex.errorName}; code: ${ex.errorCode}; desc: ${ex.errorDescription}")
            }
        } finally {
            // ... fixme
            println("closing http connection")
            conn.close()
            println("closing http connection manager")
            httpConnManager.close()

            println("closing tls context")
            tlsContext.close()

            println("closing client bootstrap")
            clientBootstrap.close()

            println("closing host resolver")
            hr.close()

            println("closing event loop group")
            elg.close()
        }
    }

    println("exiting")
}

private suspend fun HttpClientConnection.roundTrip(request: HttpRequest) {
    val streamDone = Channel<Unit>()

    // todo - we need something to make this easier
    var responseBody = byteArrayOf()

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

        override fun onResponseBody(stream: HttpStream, bodyBytesIn: Buffer): Int {
            println("onResponseBody -- recv'd ${bodyBytesIn.len} bytes")
            val contents = bodyBytesIn.readAll()

            println(contents.decodeToString())

            return contents.size
        }

        override fun onResponseComplete(stream: HttpStream, errorCode: Int) {
            println("onResponseComplete: errorCode: $errorCode")
            if (errorCode != 0) {
                val errName = CRT.awsErrorName(errorCode)
                val errDesc = CRT.awsErrorString(errorCode)
                println("error $errName: $errDesc")
            }
            streamDone.offer(Unit)
        }
    }

    val stream = makeRequest(request, responseHandler)
    try {
        stream.activate()
        // wait for completion signal
        streamDone.receive()
    } finally {
        stream.close()
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
