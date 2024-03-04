/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.http

import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.AsyncShutdown
import aws.sdk.kotlin.crt.Closeable
import aws.sdk.kotlin.crt.CrtRuntimeException
import aws.sdk.kotlin.crt.io.SocketOptions
import aws.sdk.kotlin.crt.io.requiresTls
import aws.sdk.kotlin.crt.util.asAwsByteCursor
import aws.sdk.kotlin.crt.util.free
import aws.sdk.kotlin.crt.util.initFromCursor
import aws.sdk.kotlin.crt.util.toAwsString
import kotlinx.cinterop.*
import libcrt.*

public actual class HttpClientConnectionManager actual constructor(
    public actual val options: HttpClientConnectionManagerOptions,
) : Closeable, AsyncShutdown {
    public actual val managerMetrics: HttpManagerMetrics
        get() = TODO("Not yet implemented")

    private val manager: CPointer<aws_http_connection_manager> = TODO()

    init {
        manager = memScoped {
            val endpointBytes = options.uri.host.encodeToByteArray().pin()
            val endpoint = endpointBytes.asAwsByteCursor()
            defer { endpointBytes.unpin() }

            val tlsConnOpts: aws_tls_connection_options? = if (options.uri.scheme.requiresTls()) {
                val ctx = requireNotNull(options.tlsContext) { "TlsContext is required for https"}
                val opts = alloc<aws_tls_connection_options>()
                // FIXME - TlsContext needs to be CValuesRef<aws_tls_ctx>
                // aws_tls_connection_options_init_from_ctx(opts.ptr, ctx)
                aws_tls_connection_options_set_server_name(opts.ptr, Allocator.Default, endpoint)
                defer { aws_tls_connection_options_clean_up(opts.ptr)}
                opts
            }else {
                null
            }

            val monitoringOpts: aws_http_connection_monitoring_options? = options.monitoringOptions?.let {
                val opts = alloc<aws_http_connection_monitoring_options>()
                opts.allowable_throughput_failure_interval_seconds = it.allowableThroughputFailureIntervalSeconds.convert()
                opts.minimum_throughput_bytes_per_second = it.minThroughputBytesPerSecond.convert()
                opts
            }

            // val proxyOpts: aws_http_proxy_options? = options.proxyOptions?.let { proxyOptions ->
            //     val opts = alloc<aws_http_proxy_options>()
            //     val proxyHost = proxyOptions.host.toAwsString()
            //     defer { proxyHost.free() }
            //
            //     opts.host.initFromCursor(proxyHost.asAwsByteCursor())
            //     opts.port = (proxyOptions.port ?: options.uri.scheme.defaultPort).convert()
            //
            //     val proxyTlsConnOpts: aws_tls_connection_options? = proxyOptions.tlsContext?.let { tlsCtx ->
            //         val tlsOpts = alloc<aws_tls_connection_options>()
            //         val proxyEndpoint = proxyHost.asAwsByteCursor()
            //         aws_tls_connection_options_init_from_ctx(tlsOpts.ptr, tlsCtx)
            //         aws_tls_connection_options_set_server_name(tlsOpts.ptr, Allocator.Default, proxyEndpoint)
            //
            //         defer { aws_tls_connection_options_clean_up(tlsOpts.ptr) }
            //
            //         tlsOpts
            //     }
            //
            //     opts.tls_options = proxyTlsConnOpts?.ptr
            //
            //     proxyOptions.authType.value.convert().also { opts.auth_type = it }
            //     proxyOptions.authUsername?.let {
            //         val username = it.toAwsString()
            //         defer { username.free() }
            //         opts.auth_username.initFromCursor(username.asAwsByteCursor())
            //     }
            //     proxyOptions.authPassword?.let {
            //         val pwd = it.toAwsString()
            //         defer { pwd.free() }
            //         opts.auth_password.initFromCursor(pwd.asAwsByteCursor())
            //     }
            //     opts
            // }
            //
            // val socketOpts = alloc<aws_socket_options>()
            // socketOpts.kinit(options.socketOptions)
            //
            // val managerOpts = cValue<aws_http_connection_manager_options> {
            //     bootstrap = options.clientBootstrap.ptr
            //     initial_window_size = options.initialWindowSize.convert()
            //     host.initFromCursor(endpoint)
            //     port = options.uri.port.convert()
            //
            //     max_connections = options.maxConnections.convert()
            //     enable_read_back_pressure = options.manualWindowManagement
            //     max_connection_idle_in_milliseconds = options.maxConnectionIdleMs.convert()
            //
            //     // FIXME - setup
            //     // shutdown_complete_callback = staticCFunction(::onShutdownComplete)
            //     // shutdown_complete_user_data = shutdownCompleteStableRef.asCPointer()
            //
            //     tls_connection_options = tlsConnOpts?.ptr
            //     monitoring_options = monitoringOpts?.ptr
            //     proxy_options = proxyOpts?.ptr
            //     socket_options = socketOpts.ptr
            // }
            //
            // aws_http_connection_manager_new(Allocator.Default, managerOpts)
            TODO()
        } ?: throw CrtRuntimeException("aws_http_connection_manager_new()")

    }

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

// initialize from Kotlin equivalent
private fun aws_socket_options.kinit(opts: SocketOptions) {
    type = aws_socket_type.byValue(opts.type.value.convert())
    domain = aws_socket_domain.byValue(opts.domain.value.convert())
    connect_timeout_ms = opts.connectTimeoutMs.convert()
    keep_alive_interval_sec = opts.keepAliveIntervalSecs.convert()
    keep_alive_timeout_sec = opts.keepAliveTimeoutSecs.convert()
}