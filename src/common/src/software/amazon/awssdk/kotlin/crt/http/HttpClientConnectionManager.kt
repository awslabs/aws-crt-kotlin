/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import software.amazon.awssdk.kotlin.crt.Closeable

/**
 * Manages a pool of Http connections
 */
public expect class HttpClientConnectionManager(options: HttpClientConnectionManagerOptions) : Closeable {
    // public val maxConnections: Int
    // public val windowSize: Int
    // public val uri: Uri

    /**
     * Request an HttpClientConnection from the pool
     */
    public suspend fun acquireConnection(): HttpClientConnection

    /**
     * Releases this HttpClientConnection back into the Connection Pool, and allows another Request to acquire this connection.
     * @param conn Connection to release
     */
    public fun releaseConnection(conn: HttpClientConnection)
}
