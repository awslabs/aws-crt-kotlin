/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt.http

/**
 * This class provides access to basic http connection monitoring controls in lieu of the more traditional
 * timeouts.
 *
 * The user can set a throughput threshold (in bytes per second) for the a connection to be considered healthy.  If
 * the connection falls below this threshold for a configurable amount of time, then the connection is considered
 * unhealthy and shut down.  Throughput/health is only measured when the connection has work (read or write) that
 * needs to be done.
 */
public data class HttpMonitoringOptions(
    /**
     * minimum amount of throughput, in bytes per second, for a connection to be considered healthy
     */
    val minThroughputBytesPerSecond: Int = 0,

    /**
     * How long, in seconds, a connection is allowed to be unhealthy before getting shut down.  Must be at least two
     */
    val allowableThroughputFailureIntervalSeconds: Int = 2
) {
    init {
        require(minThroughputBytesPerSecond >= 0) { "Http monitoring minimum throughput must be non-negative" }
        require(allowableThroughputFailureIntervalSeconds >= 2) { "Http monitoring failure interval must be at least two" }
    }
}
