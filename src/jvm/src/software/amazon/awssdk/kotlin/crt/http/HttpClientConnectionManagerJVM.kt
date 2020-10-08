/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import software.amazon.awssdk.kotlin.crt.Closeable

public actual class HttpClientConnectionManager actual constructor(
    public actual val options: HttpClientConnectionManagerOptions
) : Closeable {

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
        TODO("not yet implemented")
    }

    override suspend fun close() {
        TODO("Not yet implemented")
    }
}
