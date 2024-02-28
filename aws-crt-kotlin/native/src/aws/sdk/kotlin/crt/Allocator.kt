/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt

import kotlinx.cinterop.*
import libcrt.*

internal object Allocator {
    val Default: AwsAllocator = AwsAllocator()
}

@OptIn(ExperimentalForeignApi::class)
internal class AwsAllocator : NativeFreeablePlacement, CValuesRef<aws_allocator>() {
    internal val allocator: CPointer<aws_allocator> = s_crt_kotlin_allocator
        ?: throw CrtRuntimeException("CRT allocator is not initialized, ensure CRT.initRuntime() was called.")

    override fun alloc(size: Long, align: Int): NativePointed {
        val ptr = aws_mem_calloc(allocator, 1.convert(), size.convert()) ?: throw OutOfMemoryError("unable to allocate memory")
        return interpretOpaquePointed(ptr.rawValue)
    }

    override fun free(mem: NativePtr) {
        val opaque = interpretCPointer<COpaquePointerVar>(mem)
        aws_mem_release(allocator, opaque)
    }

    override fun getPointer(scope: AutofreeScope): CPointer<aws_allocator> = allocator
}

/**
 * Clean up CRT resources after K/N runtime has been released.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun cleanup() {
    aws_http_library_clean_up()
    aws_compression_library_clean_up()
    aws_io_library_clean_up()
    aws_common_library_clean_up()

    s_crt_kotlin_clean_up()
}
