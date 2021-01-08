/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import libcrt.aws_date_time
import libcrt.aws_date_time_init_now

internal actual object Platform {
    internal actual fun getenv(name: String): String? = platform.posix.getenv(name)?.toKString()

    internal actual fun epochMilliNow(): Long {
        return memScoped {
            val dt = cValue<aws_date_time>()
            aws_date_time_init_now(dt)
            dt.ptr.pointed.timestamp
        }
    }
}
