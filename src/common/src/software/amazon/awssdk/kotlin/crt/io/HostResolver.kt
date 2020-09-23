/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

internal const val DEFAULT_MAX_ENTRIES = 8

expect class HostResolver(elg: EventLoopGroup, maxEntries: Int) {

    constructor(elg: EventLoopGroup)

    companion object {
        val Default: HostResolver
    }
}
