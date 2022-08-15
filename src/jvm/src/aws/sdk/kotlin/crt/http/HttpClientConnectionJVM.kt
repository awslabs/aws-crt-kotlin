/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.http

import software.amazon.awssdk.crt.http.HttpClientConnection as HttpClientConnectionJni

/**
 * Wrapper around JNI HttpClientConnection type that implements the expected KMP interface
 */
internal class HttpClientConnectionJVM constructor(internal val jniConn: HttpClientConnectionJni) : HttpClientConnection {

    override fun makeRequest(httpReq: HttpRequest, handler: HttpStreamResponseHandler): HttpStream {
        val jniStream = jniConn.makeRequest(httpReq.into(), handler.asJniStreamResponseHandler())
        return HttpStreamJVM(jniStream)
    }

    override fun close() = jniConn.close()

    override fun shutdown() = jniConn.shutdown()
}
