/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.crt

internal actual object Platform {
    internal actual fun getenv(name: String): String? = System.getenv(name)

    internal actual fun epochMilliNow(): Long = System.currentTimeMillis()
}
