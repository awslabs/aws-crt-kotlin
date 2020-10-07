/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import software.amazon.awssdk.kotlin.crt.Closeable

internal const val DEFAULT_MAX_ENTRIES = 8

public expect class HostResolver(elg: EventLoopGroup, maxEntries: Int) : Closeable {
    public constructor(elg: EventLoopGroup)
}
