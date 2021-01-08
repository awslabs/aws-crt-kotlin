/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

/**
 * An immutable HTTP request ready to send
 * @property method The HTTP method (verb) to use (e.g. "GET", "POST", etc)
 * @property encodedPath The (percent encoded) URL path
 * @property headers The HTTP [Headers] to send with the request
 * @property body: The optional request body stream for sending a payload
 */
public data class HttpRequest(
    val method: String,
    val encodedPath: String,
    val headers: Headers,
    val body: HttpRequestBodyStream? = null
) {
    public companion object {
        public fun build(block: HttpRequestBuilder.() -> Unit): HttpRequest = HttpRequestBuilder().apply(block).build()
    }
}

/**
 * Build an immutable [HttpRequest]
 */
public class HttpRequestBuilder {
    public var method: String = "GET"

    public var encodedPath: String = ""

    public val headers: HeadersBuilder = HeadersBuilder()

    public var body: HttpRequestBodyStream? = null

    public fun build(): HttpRequest = HttpRequest(method, encodedPath, headers.build(), body)
}

/**
 * Modify the headers inside the given block
 */
public fun HttpRequestBuilder.headers(block: HeadersBuilder.() -> Unit): Unit = headers.let(block)
