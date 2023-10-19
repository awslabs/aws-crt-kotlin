/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.http

import aws.sdk.kotlin.crt.AsyncShutdown
import aws.sdk.kotlin.crt.Closeable

public actual class HttpClientConnectionManager actual constructor(
    public actual val options: HttpClientConnectionManagerOptions,
) : Closeable, AsyncShutdown {
    /**
     * Request an HttpClientConnection from the pool
     */
    public actual suspend fun acquireConnection(): HttpClientConnection {
        TODO("Not yet implemented")
    }

    /**
     * Releases this HttpClientConnection back into the Connection Pool, and allows another Request to acquire this connection.
     * @param conn Connection to release
     */
    public actual fun releaseConnection(conn: HttpClientConnection) {
    }

    override suspend fun waitForShutdown() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}