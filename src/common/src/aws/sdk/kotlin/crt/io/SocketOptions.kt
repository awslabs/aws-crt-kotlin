/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

/**
 * Socket communications domain
 */
public enum class SocketDomain(public val value: Int) {
    /**
     * Corresponds to PF_INET in Berkeley sockets
     */
    IPv4(0),

    /**
     * Corresponds to PF_INET6 in Berkeley sockets
     */
    IPv6(1),

    /**
     * Corresponds to PF_LOCAL in Berkeley sockets, usually UNIX domain sockets or named pipes
     */
    LOCAL(2),
}

/**
 * Socket type
 */
public enum class SocketType(public val value: Int) {
    /**
     * Corresponds to SOCK_STREAM in Berkeley sockets (TCP)
     */
    STREAM(0),

    /**
     * Corresponds to SOCK_DGRAM in Berkeley sockets (UDP)
     */
    DGRAM(1),
}

/**
 * This class is a kotlin equivalent placeholder for aws_socket_options
 * from aws-c-io to provide access to TCP/UDP socket configuration.
 *
 * @property domain the socket domain
 * @property type the socket type
 * @property connectTimeoutMs the number of milliseconds before a connection attempt will be considered timed out
 * @property keepAliveIntervalSecs the number of seconds between TCP keepalive packets being sent to the peer.
 * 0 disables keepalive
 * @property keepAliveTimeoutSecs the number of seconds to wait for a keepalive response before considering the
 * connection timed out. 0 disables keepalive
 */
public data class SocketOptions(
    val domain: SocketDomain = SocketDomain.IPv6,
    val type: SocketType = SocketType.STREAM,
    val connectTimeoutMs: Int = 3000,
    val keepAliveIntervalSecs: Int = 0,
    val keepAliveTimeoutSecs: Int = 0,
)
