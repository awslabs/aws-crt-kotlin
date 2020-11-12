/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import kotlinx.coroutines.future.await
import software.amazon.awssdk.kotlin.crt.AsyncShutdown
import software.amazon.awssdk.kotlin.crt.Closeable
import software.amazon.awssdk.kotlin.crt.io.SocketDomain
import software.amazon.awssdk.kotlin.crt.io.SocketOptions
import software.amazon.awssdk.kotlin.crt.io.SocketType
import java.net.URI
import software.amazon.awssdk.crt.http.HttpClientConnectionManager as HttpClientConnectionManagerJni
import software.amazon.awssdk.crt.http.HttpClientConnectionManagerOptions as HttpClientConnectionManagerOptionsJni
import software.amazon.awssdk.crt.http.HttpMonitoringOptions as HttpMonitoringOptionsJni
import software.amazon.awssdk.crt.http.HttpProxyOptions as HttpProxyOptionsJni
import software.amazon.awssdk.crt.io.SocketOptions as SocketOptionsJni

public actual class HttpClientConnectionManager actual constructor(
    public actual val options: HttpClientConnectionManagerOptions
) : Closeable, AsyncShutdown {

    private val jniManager = HttpClientConnectionManagerJni.create(options.into())

    /**
     * Request an HttpClientConnection from the pool
     */
    public actual suspend fun acquireConnection(): HttpClientConnection {
        val connFuture = jniManager.acquireConnection()
        val jniConn = connFuture.await()
        return HttpClientConnectionJVM(jniConn)
    }

    /**
     * Releases this HttpClientConnection back into the Connection Pool, and allows another Request to acquire this connection.
     * @param conn Connection to release
     */
    public actual fun releaseConnection(conn: HttpClientConnection) {
        val ktConn = conn as? HttpClientConnectionJVM ?: return
        jniManager.releaseConnection(ktConn.jniConn)
    }

    override fun close() {
        jniManager.close()
    }

    override suspend fun waitForShutdown() {
        jniManager.shutdownCompleteFuture.await()
    }
}

private fun HttpClientConnectionManagerOptions.into(): HttpClientConnectionManagerOptionsJni {
    val jniOpts = HttpClientConnectionManagerOptionsJni()
        .withUri(URI.create(uri.toString()))
        .withClientBootstrap(clientBootstrap.jniBootstrap)
        .withSocketOptions(socketOptions.into())
        .withWindowSize(initialWindowSize)
        .withManualWindowManagement(manualWindowManagement)
        .withMaxConnections(maxConnections)
        .withMaxConnectionIdleInMilliseconds(maxConnectionIdleMs)

    if (tlsContext != null) jniOpts.withTlsContext(tlsContext!!.jniCtx)
    if (proxyOptions != null) jniOpts.withProxyOptions(proxyOptions!!.into())
    if (monitoringOptions != null) jniOpts.withMonitoringOptions(monitoringOptions!!.into())

    return jniOpts
}

private fun SocketOptions.into(): SocketOptionsJni {
    val jniOpts = SocketOptionsJni()
    jniOpts.connectTimeoutMs = connectTimeoutMs
    jniOpts.keepAliveIntervalSecs = keepAliveIntervalSecs
    jniOpts.keepAliveTimeoutSecs = keepAliveTimeoutSecs
    jniOpts.type = when (type) {
        SocketType.STREAM -> software.amazon.awssdk.crt.io.SocketOptions.SocketType.STREAM
        SocketType.DGRAM -> software.amazon.awssdk.crt.io.SocketOptions.SocketType.DGRAM
    }

    jniOpts.domain = when (domain) {
        SocketDomain.IPv4 -> software.amazon.awssdk.crt.io.SocketOptions.SocketDomain.IPv4
        SocketDomain.IPv6 -> software.amazon.awssdk.crt.io.SocketOptions.SocketDomain.IPv6
        SocketDomain.LOCAL -> software.amazon.awssdk.crt.io.SocketOptions.SocketDomain.LOCAL
    }

    return jniOpts
}
private fun HttpProxyOptions.into(): HttpProxyOptionsJni {
    val jniOpts = HttpProxyOptionsJni()
    jniOpts.host = host
    if (port != null) jniOpts.port = port!!

    jniOpts.authorizationUsername = authUsername
    jniOpts.authorizationPassword = authPassword

    jniOpts.authorizationType = when (authType) {
        HttpProxyAuthorizationType.None -> software.amazon.awssdk.crt.http.HttpProxyOptions.HttpProxyAuthorizationType.None
        HttpProxyAuthorizationType.Basic -> software.amazon.awssdk.crt.http.HttpProxyOptions.HttpProxyAuthorizationType.Basic
    }

    tlsContext?.let { jniOpts.tlsContext = it.jniCtx }

    return jniOpts
}
private fun HttpMonitoringOptions.into(): HttpMonitoringOptionsJni {
    val jniOpts = HttpMonitoringOptionsJni()
    jniOpts.allowableThroughputFailureIntervalSeconds = allowableThroughputFailureIntervalSeconds
    jniOpts.minThroughputBytesPerSecond = minThroughputBytesPerSecond.toLong()
    return jniOpts
}
