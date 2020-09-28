/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import software.amazon.awssdk.kotlin.crt.io.ClientBootstrap
import software.amazon.awssdk.kotlin.crt.io.SocketOptions
import software.amazon.awssdk.kotlin.crt.io.TlsContext

/**
 * Configuration options for an [HttpPoolConnectionManager] instance
 */
public class HttpClientConnectionManagerOptions internal constructor(
    builder: HttpClientConnectionManagerOptionsBuilder
) {
    public val uri: Uri = requireNotNull(builder.uri) { "URI is required" }
    public val clientBootstrap: ClientBootstrap = requireNotNull(builder.clientBootstrap) { "ClientBootstrap is required" }
    public val socketOptions: SocketOptions = requireNotNull(builder.socketOptions) { "SocketOptions is required" }
    public val tlsContext: TlsContext? = builder.tlsContext
    public val windowSize: Int = builder.windowSize
    public val bufferSize: Int = builder.bufferSize
    public val maxConnections: Int = builder.maxConnections
    public val proxyOptions: HttpProxyOptions? = builder.proxyOptions
    public val manualWindowManagement: Boolean = builder.manualWindowManagement
    public val monitoringOptions: HttpMonitoringOptions? = builder.monitoringOptions
    public val maxConnectionIdleMs: Long = builder.maxConnectionIdleMs

    init {
        require(windowSize > 0) { "Window size must be > 0" }
        require(bufferSize > 0) { "Buffer size must be > 0" }
        require(maxConnections > 0) { "Max connections must be > 0" }
        require(uri.scheme.isHttp()) { "URI has an unknown scheme: ${uri.scheme}" }

        if (uri.scheme.requiresTls()) {
            requireNotNull(tlsContext) { "TlsContext required by URI scheme: ${uri.scheme}" }
        }
    }

    public companion object {
        public const val DEFAULT_MAX_BUFFER_SIZE: Int = 16 * 1024
        public const val DEFAULT_MAX_WINDOW_SIZE: Int = Int.MAX_VALUE
        public const val DEFAULT_MAX_CONNECTIONS: Int = 2

        public fun build(block: HttpClientConnectionManagerOptionsBuilder.() -> Unit): HttpClientConnectionManagerOptions =
            HttpClientConnectionManagerOptionsBuilder().apply(block).build()
    }
}

public class HttpClientConnectionManagerOptionsBuilder {
    internal fun build(): HttpClientConnectionManagerOptions = HttpClientConnectionManagerOptions(this)

    internal var uri: Uri? = null

    public fun uri(block: UriBuilder.() -> Unit) {
        uri = UriBuilder().apply(block).build()
    }

    public var clientBootstrap: ClientBootstrap? = null

    public var socketOptions: SocketOptions? = null

    public var tlsContext: TlsContext? = null

    public var windowSize: Int = HttpClientConnectionManagerOptions.DEFAULT_MAX_WINDOW_SIZE

    public var bufferSize: Int = HttpClientConnectionManagerOptions.DEFAULT_MAX_BUFFER_SIZE

    public var maxConnections: Int = HttpClientConnectionManagerOptions.DEFAULT_MAX_CONNECTIONS

    public var proxyOptions: HttpProxyOptions? = null

    public var manualWindowManagement: Boolean = false

    public var monitoringOptions: HttpMonitoringOptions? = null

    public var maxConnectionIdleMs: Long = 0
}
