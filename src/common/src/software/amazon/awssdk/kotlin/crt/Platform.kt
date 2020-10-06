/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

/**
 * Platform specific functionality
 */
internal expect object Platform {

    /**
     * Get an environment variable by name
     */
    internal fun getenv(name: String): String?
}
