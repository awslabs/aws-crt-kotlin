/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

actual class HostResolver actual constructor(elg: EventLoopGroup, maxEntries: Int) {

    actual constructor(elg: EventLoopGroup) : this(elg, DEFAULT_MAX_ENTRIES) {
        TODO("Not yet implemented")
    }

    actual companion object {
        actual val Default: HostResolver
            get() = TODO("Not yet implemented")
    }
}
