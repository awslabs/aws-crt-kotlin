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
    internal val allocator: CPointer<aws_allocator>

    init {
        val traceLevel = CrtDebug.traceLevel
        println("trace level: $traceLevel")
        var tmp = aws_default_allocator() ?: throw CrtRuntimeException("default allocator init failed")
        if (traceLevel > 0) {
            tmp = aws_mem_tracer_new(tmp, null, traceLevel.convert(), FRAMES_PER_STACK.convert()) ?: throw CrtRuntimeException("aws_mem_tracer_new()")
        }
        allocator = tmp
    }

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
    // IT'S THE FINAL COUNTDOWN..dun-nuh-nuh-na, ok but seriously let's try and tidy up before the world ends
    aws_http_library_clean_up()
    aws_compression_library_clean_up()
    aws_io_library_clean_up()
    aws_common_library_clean_up()

    if (CrtDebug.traceLevel > 0) {
        println("dumping memtrace")
        aws_mem_tracer_dump(Allocator.Default.allocator)
    }

    // cleanup logging
    Log.cleanup()

    if (CrtDebug.traceLevel > 0) {
        aws_mem_tracer_destroy(Allocator.Default.allocator)
    }
}
