/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt.io

import aws.sdk.kotlin.runtime.crt.AsyncShutdown
import aws.sdk.kotlin.runtime.crt.Closeable

internal const val DEFAULT_MAX_ENTRIES = 8

public expect class HostResolver(elg: EventLoopGroup, maxEntries: Int) : Closeable, AsyncShutdown {
    public constructor(elg: EventLoopGroup)
}
