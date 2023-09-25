/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.crt.CRT
import aws.sdk.kotlin.crt.CrtRuntimeException
import aws.sdk.kotlin.crt.LogDestination
import aws.sdk.kotlin.crt.http.*
import aws.sdk.kotlin.crt.io.*
import kotlinx.coroutines.channels.Channel

fun main(args: Array<String>) {
    platformInit()

    val opts = CliOpts.from(args)

    CRT.initRuntime {
        logLovel = opts.logLevel
        logDestination = LogDestination.Stderr
        if (opts.traceFile != null) {
            logDestination = LogDestination.File
            logFile = opts.traceFile
        }
    }

    println("url: ${opts.url}")
    val uri = Uri.parse(opts.url)
    println("parsed uri: $uri")
    println("headers: ${opts.headers}")

    val sink = if (opts.outputFile != null) createFileSink(opts.outputFile!!) else StdoutSink()

    val tlsContextBuilder = TlsContextOptionsBuilder()

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

    val socketOpts = SocketOptions(connectTimeoutMs = opts.connectTimeout)
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

        headers {
            // manually add a user-agent and host header
            if (!contains("User-Agent")) append("User-Agent", "elasticurl_kotlin 1.0, Powered by the AWS Common Runtime.")
            if (!contains("Host")) append("Host", uri.hostAndPort)
        }
    }

    runBlocking {
        val conn = httpConnManager.acquireConnection()

        try {
            conn.roundTrip(request, sink)
        } catch (ex: Exception) {
            println("failed to round trip request: ${ex.message}")
            if (ex is CrtRuntimeException) {
                println("CrtException: name: ${ex.errorName}; code: ${ex.errorCode}; desc: ${ex.errorDescription}")
            }
        } finally {
            // ... fixme - need to define the resource management story
            println("closing http connection")
            conn.close()
            println("closing http connection manager")
            httpConnManager.close()
            httpConnManager.waitForShutdown()

            println("closing tls context")
            tlsContext.close()

            println("closing client bootstrap")
            clientBootstrap.close()
            clientBootstrap.waitForShutdown()

            println("closing host resolver")
            hr.close()
            hr.waitForShutdown()

            println("closing event loop group")
            elg.close()
            elg.waitForShutdown()

            sink.close()
        }
    }

    println("exiting")
}

private suspend fun HttpClientConnection.roundTrip(request: HttpRequest, sink: Sink) {
    val streamDone = Channel<Unit>()

    val responseHandler = object : HttpStreamResponseHandler {
        override fun onResponseHeaders(
            stream: HttpStream,
            responseStatusCode: Int,
            blockType: Int,
            nextHeaders: List<HttpHeader>?,
        ) {
            println("onResponseHeaders -- status: $responseStatusCode ")
            println("headers:")
            nextHeaders?.forEach { println("\t${it.name}: ${it.value}") }
        }

        override fun onResponseBody(stream: HttpStream, bodyBytesIn: Buffer): Int {
            println("onResponseBody -- recv'd ${bodyBytesIn.len} bytes")
            val contents = bodyBytesIn.readAll()
            // println(contents.decodeToString())
            sink.write(contents)

            return contents.size
        }

        override fun onResponseComplete(stream: HttpStream, errorCode: Int) {
            println("onResponseComplete: errorCode: $errorCode")
            if (errorCode != 0) {
                val errName = CRT.errorName(errorCode)
                val errDesc = CRT.errorString(errorCode)
                println("error $errName: $errDesc")
            }
            streamDone.trySend(Unit)
            // has to be explicitly closed or it leaks in K/N
            streamDone.close()
        }
    }

    val stream = makeRequest(request, responseHandler)
    try {
        stream.activate()
        // wait for completion signal
        streamDone.receiveCatching()
    } finally {
        stream.close()
    }
}
