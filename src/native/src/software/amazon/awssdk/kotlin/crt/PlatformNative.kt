/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.toKString

internal actual object Platform {
    internal actual fun getenv(name: String): String? = platform.posix.getenv(name)?.toKString()
}