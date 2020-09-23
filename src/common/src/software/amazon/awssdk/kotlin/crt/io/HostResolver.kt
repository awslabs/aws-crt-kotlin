/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

internal const val DEFAULT_MAX_ENTRIES = 8

public expect class HostResolver(elg: EventLoopGroup, maxEntries: Int) {

    public constructor(elg: EventLoopGroup)

    public companion object {
        public val Default: HostResolver
    }
}
