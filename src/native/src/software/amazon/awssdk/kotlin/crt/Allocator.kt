/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.*
import libcrt.*

private const val FRAMES_PER_STACK = 8

internal object Allocator {
    val Default: AwsAllocator = AwsAllocator()
}

internal class AwsAllocator : NativeFreeablePlacement, CValuesRef<aws_allocator>() {
    internal val allocator: CPointer<aws_allocator> = s_crt_kotlin_allocator ?: throw CrtRuntimeException("CRT allocator was not initialized. Have you called CRT.initRuntime()?")

    override fun getPointer(scope: AutofreeScope): CPointer<aws_allocator> = allocator

    override fun alloc(size: Long, align: Int): NativePointed {
        val ptr = aws_mem_calloc(allocator, 1.convert(), size.convert())
            ?: throw OutOfMemoryError("unable to allocate native memory")
        return interpretOpaquePointed(ptr.rawValue)
    }

    override fun free(mem: NativePtr) {
        val opaque = interpretCPointer<COpaquePointerVar>(mem)
        aws_mem_release(allocator, opaque)
    }
}

internal fun finalCleanup() {
    // IT'S THE FINAL COUNTDOWN..dun-nuh-nuh-na, ok but seriously let's try and tidy up before the world ends.
    // By the time we get to this function the k/n runtime has been _torn down_. Accessing any top-level
    // variable or object requires re-initializing the runtime (via initRuntime()) but that defeats the purpose,
    // everything we have left to do is cleanup CRT resources of which k/n knows nothing about.
    aws_http_library_clean_up()
    aws_compression_library_clean_up()
    aws_io_library_clean_up()
    aws_common_library_clean_up()

    // crt.def
    s_crt_kotlin_clean_up()
}
