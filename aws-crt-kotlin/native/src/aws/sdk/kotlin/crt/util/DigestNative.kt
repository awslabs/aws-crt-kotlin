/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.util

import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.CrtRuntimeException
import aws.sdk.kotlin.crt.awsAssertOpSuccess
import kotlinx.cinterop.*
import libcrt.*

/**
 * Utility object for various hash functions
 */
public actual object Digest {
    /**
     * Calculate the SHA-256 hash of the input [buffer]
     */
    public actual fun sha256(buffer: ByteArray): ByteArray = (::aws_sha256_compute)(buffer)

    /**
     * Calculate the SHA-1 hash of the input [buffer]
     */
    public actual fun sha1(buffer: ByteArray): ByteArray = (::aws_sha1_compute)(buffer)
}

/**
 * A typealias for the hash function signature used by CRT
 */
internal typealias NativeComputeHashFn = (
    allocator: CValuesRef<aws_allocator>?,
    input: CValuesRef<aws_byte_cursor>?,
    output: CValuesRef<aws_byte_buf>?,
    truncate_to: platform.posix.size_t,
) -> Int

/**
 * Given a [ByteArray], invoke the given [NativeComputeHashFn] with the appropriate native arguments.
 * @param input the input [ByteArray] to be hashed
 * @return a [ByteArray] representing the hash of the [input]
 */
private operator fun NativeComputeHashFn.invoke(input: ByteArray): ByteArray = memScoped {
    val inputCursor = input.usePinned {
        aws_byte_cursor_from_array(it.addressOf(0), input.size.convert())
    }

    val output: CPointer<aws_byte_buf> = aws_mem_calloc(
        Allocator.Default,
        1.convert(),
        sizeOf<aws_byte_buf>().convert(),
    )?.reinterpret() ?: throw CrtRuntimeException("aws_mem_calloc() aws_byte_buf")
    aws_byte_buf_init(output, Allocator.Default.allocator, 10_000U)

    awsAssertOpSuccess(
        this@invoke.invoke(
            Allocator.Default.allocator,
            inputCursor,
            output,
            0U,
        ),
    ) { "failed to invoke hash function" }

    checkNotNull(output.pointed.buffer) { "expected output buffer to be non-null" }
    return output.pointed.buffer!!.readBytes(output.pointed.len.toInt())
}
