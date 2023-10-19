/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt

internal actual object Platform {
    /**
     * Get an environment variable by name
     */
    internal actual fun getenv(name: String): String? {
        TODO("Not yet implemented")
    }

    /**
     * Get the current time in epoch milliseconds
     */
    internal actual fun epochMilliNow(): Long {
        TODO("Not yet implemented")
    }
}
