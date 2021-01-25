/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.AsyncShutdown
import aws.sdk.kotlin.crt.Closeable
import software.amazon.awssdk.crt.io.HostResolver as HostResolverJni

public actual class HostResolver actual constructor(elg: EventLoopGroup, maxEntries: Int) : Closeable,
    aws.sdk.kotlin.crt.AsyncShutdown {
    internal val jniHr = HostResolverJni(elg.jniElg, maxEntries)

    public actual constructor(elg: EventLoopGroup) : this(elg, DEFAULT_MAX_ENTRIES)

    override fun close() {
        jniHr.close()
    }

    override suspend fun waitForShutdown() {
        // TODO jni HostResolver is missing a shutdown complete future...
    }
}
