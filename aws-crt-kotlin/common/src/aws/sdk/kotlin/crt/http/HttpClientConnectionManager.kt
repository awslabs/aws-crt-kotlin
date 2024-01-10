/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.http

import aws.sdk.kotlin.crt.AsyncShutdown
import aws.sdk.kotlin.crt.Closeable

/**
 * Manages a pool of Http connections
 */
public expect class HttpClientConnectionManager(options: HttpClientConnectionManagerOptions) :
    Closeable,
    AsyncShutdown {
    /**
     * The active metrics for this connection manager
     */
    public val managerMetrics: HttpManagerMetrics

    /**
     * The options this manager was configured with
     */
    public val options: HttpClientConnectionManagerOptions

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
