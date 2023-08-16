/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt

/**
 * Platform specific functionality
 */
internal expect object Platform {

    /**
     * Get an environment variable by name
     */
    internal fun getenv(name: String): String?

    /**
     * Get the current time in epoch milliseconds
     */
    internal fun epochMilliNow(): Long
}

/**
 * Get the current time in epoch seconds
 */
internal fun Platform.epochNow(): Long = epochMilliNow() / 1000
