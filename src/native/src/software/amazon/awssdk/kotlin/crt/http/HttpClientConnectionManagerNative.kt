/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import kotlinx.cinterop.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.receiveOrNull
import libcrt.*
import software.amazon.awssdk.kotlin.crt.*
import software.amazon.awssdk.kotlin.crt.Allocator
import software.amazon.awssdk.kotlin.crt.io.SocketOptions
import kotlin.native.concurrent.freeze

private typealias ShutdownChannel = Channel<Unit>
private typealias ConnectionAcquisitionChannel = Channel<HttpConnectionAcquisition>

private fun onShutdownComplete(userdata: COpaquePointer?) {
    // initRuntimeIfNeeded()
    if (userdata != null) {
        val notify = userdata.asStableRef<ShutdownChannel>().get()
        notify.offer(Unit)
        notify.close()
    }
}

private data class HttpConnectionAcquisition(val conn: CPointer<aws_http_connection>?, val errCode: Int)

private fun onConnectionAcquired(
    conn: CPointer<aws_http_connection>?,
    errCode: Int,
    userdata: COpaquePointer?
) {
    if (userdata != null) {
        val notify = userdata.asStableRef<ConnectionAcquisitionChannel>().get()
        val acquisition = HttpConnectionAcquisition(conn, errCode)
        notify.offer(acquisition)
        notify.close()
    }
}

// TODO - port over tests from crt-java

public actual class HttpClientConnectionManager actual constructor(options: HttpClientConnectionManagerOptions) :
    Closeable, CrtResource<aws_http_connection_manager>() {
    private val manager: CPointer<aws_http_connection_manager>

    private val shutdownComplete: ShutdownChannel = Channel<Unit>(0).freeze()
    private val shutdownCompleteStableRef = StableRef.create(shutdownComplete)

    init {
        val endpoint = options.uri.host.asAwsByteCursor()

        // FIXME - Quite a few of these params are copied in the manager_new() impl and would otherwise
        // be ok being stack allocated but I'm not seeing a way to get stack allocated pointers in Kotlin/Native...
        // for now we are allocating these on the heap since I can't figure out how to get CPointer<T>
        // out of CValue<T>. We might need a convenience function in the .def file

        val tlsConnOpts: aws_tls_connection_options? = options.tlsContext?.let {
            val opts = Allocator.Default.alloc<aws_tls_connection_options>()

            aws_tls_connection_options_init_from_ctx(opts.ptr, it)
            aws_tls_connection_options_set_server_name(opts.ptr, Allocator.Default, endpoint)
            opts
        }

        val monitoringOpts: aws_http_connection_monitoring_options? = options.monitoringOptions?.let {
            val opts = Allocator.Default.alloc<aws_http_connection_monitoring_options>()
            opts.allowable_throughput_failure_interval_seconds = it.allowableThroughputFailureIntervalSeconds.convert()
            opts.minimum_throughput_bytes_per_second = it.minThroughputBytesPerSecond.convert()
            opts
        }

        val proxyOpts: aws_http_proxy_options? = options.proxyOptions?.let { proxyOptions ->
            val opts = Allocator.Default.alloc<aws_http_proxy_options>()
            opts.host.initFromString(proxyOptions.host)
            opts.port = (proxyOptions.port ?: options.uri.scheme.defaultPort).convert()

            val proxyTlsConnOpts: aws_tls_connection_options? = proxyOptions.tlsContext?.let { tlsCtx ->
                val tlsOpts = Allocator.Default.alloc<aws_tls_connection_options>()
                val proxyEndpoint = proxyOptions.host.asAwsByteCursor()
                aws_tls_connection_options_init_from_ctx(tlsOpts.ptr, tlsCtx)
                aws_tls_connection_options_set_server_name(tlsOpts.ptr, Allocator.Default, proxyEndpoint)
                tlsOpts
            }

            opts.tls_options = proxyTlsConnOpts?.ptr

            opts.auth_type = proxyOptions.authType.value.convert()
            proxyOptions.authUsername?.let { opts.auth_username.initFromString(it) }
            proxyOptions.authPassword?.let { opts.auth_password.initFromString(it) }
            opts
        }

        val socketOpts = Allocator.Default.alloc<aws_socket_options>()
        socketOpts.kinit(options.socketOptions)

        val managerOpts = cValue<aws_http_connection_manager_options> {
            bootstrap = options.clientBootstrap.ptr
            initial_window_size = options.windowSize.convert()
            tls_connection_options = null
            host.initFromCursor(endpoint)
            port = options.uri.port.convert()

            enable_read_back_pressure = options.manualWindowManagement
            max_connection_idle_in_milliseconds = options.maxConnectionIdleMs.convert()

            shutdown_complete_callback = staticCFunction(::onShutdownComplete)
            shutdown_complete_user_data = shutdownCompleteStableRef.asCPointer()

            tls_connection_options = tlsConnOpts?.ptr
            monitoring_options = monitoringOpts?.ptr
            proxy_options = proxyOpts?.ptr
            socket_options = socketOpts.ptr
        }

        val tmp = aws_http_connection_manager_new(Allocator.Default, managerOpts)

        tlsConnOpts?.let {
            aws_tls_connection_options_clean_up(it.ptr)
            Allocator.Default.free(it)
        }

        monitoringOpts?.let { Allocator.Default.free(it) }

        proxyOpts?.let {
            it.tls_options?.let { proxyTlsOpts ->
                aws_tls_connection_options_clean_up(proxyTlsOpts)
                Allocator.Default.free(proxyTlsOpts)
            }
            Allocator.Default.free(it)
        }

        Allocator.Default.free(socketOpts)

        manager = tmp ?: throw CrtRuntimeException("aws_http_connection_manager_new()")
    }

    override val ptr: CPointer<aws_http_connection_manager> = manager

    /**
     * Request an HttpClientConnection from the pool
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    public actual suspend fun acquireConnection(): HttpClientConnection {
        val notify: ConnectionAcquisitionChannel = Channel<HttpConnectionAcquisition>(1).freeze()
        val stableRef = StableRef.create(notify)
        val cb = staticCFunction(::onConnectionAcquired)
        aws_http_connection_manager_acquire_connection(
            manager,
            cb,
            stableRef.asCPointer()
        )
        val acquisition = notify.receive()
        if (acquisition.errCode != AWS_OP_SUCCESS) {
            throw HttpException(acquisition.errCode)
        }

        if (acquisition.conn == null) {
            throw CrtRuntimeException("acquireConnection(): http connection cannot be null")
        }

        return HttpClientConnectionNative(this, acquisition.conn)
    }

    /**
     * Releases this HttpClientConnection back into the Connection Pool, and allows another Request to acquire this connection.
     * @param conn Connection to release
     */
    public actual fun releaseConnection(conn: HttpClientConnection) {
        val nativeConn = conn as HttpClientConnectionNative
        aws_http_connection_manager_release_connection(manager, nativeConn)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun close() {
        // TODO - deal with close() being called and clearing out pending acquisitions
        shutdownComplete.receiveOrNull()
        shutdownCompleteStableRef.dispose()
        aws_http_connection_manager_release(manager)
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
