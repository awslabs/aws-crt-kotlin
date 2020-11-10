/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import software.amazon.awssdk.kotlin.crt.Closeable
import software.amazon.awssdk.crt.io.HostResolver as HostResolverJni

public actual class HostResolver actual constructor(elg: EventLoopGroup, maxEntries: Int) : Closeable {
    internal val jniHr = HostResolverJni(elg.jniElg, maxEntries)

    public actual constructor(elg: EventLoopGroup) : this(elg, DEFAULT_MAX_ENTRIES)

    override suspend fun close() {
        jniHr.close()
    }
}
