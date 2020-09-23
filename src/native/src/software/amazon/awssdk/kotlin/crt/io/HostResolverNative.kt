/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import libcrt.aws_host_resolver
import libcrt.aws_host_resolver_new_default
import libcrt.aws_host_resolver_release
import software.amazon.awssdk.kotlin.crt.Allocator
import software.amazon.awssdk.kotlin.crt.Closeable
import software.amazon.awssdk.kotlin.crt.CrtResource
import software.amazon.awssdk.kotlin.crt.CrtRuntimeException

public actual class HostResolver actual constructor(
    elg: EventLoopGroup,
    maxEntries: Int
) : CrtResource<aws_host_resolver>(), Closeable {

    private val resolver: CPointer<aws_host_resolver>

    init {
        resolver = aws_host_resolver_new_default(
            Allocator.Default, maxEntries.convert(),
            elg,
            null
        ) ?: throw CrtRuntimeException("aws_host_resolver_new_default() failed")
    }

    public actual constructor(elg: EventLoopGroup) : this(elg, DEFAULT_MAX_ENTRIES)

    override val ptr: CPointer<aws_host_resolver>
        get() = resolver

    public actual companion object {
        public actual val Default: HostResolver by lazy {
            HostResolver(EventLoopGroup.Default)
        }
    }

    override suspend fun close() {
        aws_host_resolver_release(resolver)
    }
}
