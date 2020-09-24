/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.*
import libcrt.*
import platform.posix.getenv

private const val FRAMES_PER_STACK = 8

internal object Allocator {
    val Default: AwsAllocator = AwsAllocator()
}

internal class AwsAllocator : NativeFreeablePlacement, CValuesRef<aws_allocator>() {
    private val allocator: CPointer<aws_allocator>

    init {
        val traceLevel = getenv("aws.crt.memory.tracing")?.toKString()?.toIntOrNull()
        var tmp = aws_default_allocator() ?: throw CrtRuntimeException("default allocator init failed")
        if (traceLevel != null) {
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

// FIXME - arena api's are not open enough to do quite what we want...
// /**
//  * Runs given [block] providing allocation of memory
//  * which will be automatically disposed at the end of this scope.
//  */
// internal fun awsMemScoped(allocator: AwsAllocator? = null, block: Arena.() -> Unit) {
//     val arena = Arena(allocator ?: Allocator.Default)
//     try {
//         arena.block()
//     } finally {
//         arena.clear()
//     }
// }
