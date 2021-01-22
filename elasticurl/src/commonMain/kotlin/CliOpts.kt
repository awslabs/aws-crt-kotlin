/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

import kotlinx.cli.*
import aws.sdk.kotlin.runtime.crt.LogLevel

private const val DEFAULT_CONNECT_TIMEOUT_MS = 3000

private class FileArgtype(val isDirectory: kotlin.Boolean = false) : ArgType<String>(true) {
    override val description: kotlin.String
        get() = if (isDirectory) "{ PATH }" else "{ FILE }"

    override fun convert(value: kotlin.String, name: kotlin.String): kotlin.String = value
}

enum class HttpMethod { GET, POST, PUT, HEAD, DELETE }

class CliOpts {
    companion object {
        fun from(args: Array<String>): CliOpts = CliOpts().apply { parse(args) }
    }
    private val parser = ArgParser("elasticurl")

    val url: String by parser.argument(ArgType.String, description = "URL to make request to")
    val cacert: String? by parser.option(FileArgtype(), description = "path to CA certificate file")
    val capath: String? by parser.option(FileArgtype(true), description = "path to CA certificate file")

    val cert: String? by parser.option(FileArgtype(), description = "path to a PEM encoded certificate to use with mTLS")
    val key: String? by parser.option(FileArgtype(), description = "path to a PEM encoded private key that matches cert")
    val connectTimeout: Int by parser.option(ArgType.Int, fullName = "connect-timeout", description = "time in milliseconds to wait for a connection").default(DEFAULT_CONNECT_TIMEOUT_MS)

    val headers: List<String>? by parser.option(ArgType.String, fullName = "header", shortName = "H", description = "additional headers to send with the request of the form `key:value`").multiple()
    val includeHeaders: Boolean by parser.option(ArgType.Boolean, fullName = "include", shortName = "i", description = "include headers in output").default(false)

    val data: String? by parser.option(ArgType.String, shortName = "d", description = "data to POST or PUT")
    val dataFile: String? by parser.option(FileArgtype(), fullName = "data-file", description = "file to read data from")

    private val rawHttpMethod: HttpMethod by parser.option(ArgType.Choice<HttpMethod>(), fullName = "method", shortName = "M", description = "HTTP method to use for the request").default(HttpMethod.GET)
    private val useHttpGet: Boolean? by parser.option(ArgType.Boolean, fullName = "get", shortName = "G", description = "uses GET for http verb/method")
    private val useHttpPost: Boolean? by parser.option(ArgType.Boolean, fullName = "post", shortName = "P", description = "uses POST for http verb/method")
    private val useHttpHead: Boolean? by parser.option(ArgType.Boolean, fullName = "head", shortName = "I", description = "uses HEAD for http verb/method")

    val insecure: Boolean by parser.option(ArgType.Boolean, shortName = "k", description = "turn off TLS validation").default(false)
    val outputFile: String? by parser.option(FileArgtype(), fullName = "output", shortName = "o", description = "dumps content-body to FILE instead of stdout")
    val traceFile: String? by parser.option(FileArgtype(), fullName = "trace", shortName = "t", description = "dumps logs to FILE instead of stderr")
    val logLevel: LogLevel by parser.option(ArgType.Choice<LogLevel>(), fullName = "verbose", shortName = "v", description = "log level to configure").default(LogLevel.None)

    val requireHttp1: Boolean by parser.option(ArgType.Boolean, fullName = "http1_1", description = "HTTP/1.1 connection required").default(false)
    val requireHttp2: Boolean by parser.option(ArgType.Boolean, fullName = "http2", description = "HTTP/2 connection required").default(false)

    fun parse(args: Array<String>) = parser.parse(args)

    val httpMethod: HttpMethod
        get() = when {
            useHttpGet == true -> HttpMethod.GET
            useHttpPost == true -> HttpMethod.POST
            useHttpHead == true -> HttpMethod.HEAD
            else -> rawHttpMethod
        }
}
