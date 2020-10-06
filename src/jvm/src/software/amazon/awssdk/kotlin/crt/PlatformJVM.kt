/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.amazon.awssdk.kotlin.crt

internal actual object Platform {
    internal actual fun getenv(name: String): String? = System.getenv(name)
}
