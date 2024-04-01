/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.CrtRuntimeException
import aws.sdk.kotlin.crt.awsAssertOpSuccess
import kotlinx.cinterop.*
import libcrt.*

/**
 * A typealias to CRT's aws_<checksum_algorithm>_new functions (i.e aws_sha1_new, aws_sha256_new))
 */
internal typealias InitializeHashFn = (
    allocator: CValuesRef<aws_allocator>?,
) -> CPointer<aws_hash>?

public open class ShaNative(
    public val initializeFn: InitializeHashFn,
) {
    private var hash: CPointer<aws_hash>

    init {
        hash = initializeHash()
    }

    // aws_hash_update
    public fun update(input: ByteArray, offset: Int, length: Int) {
        val inputCursor = input.usePinned {
            aws_byte_cursor_from_array(it.addressOf(offset), length.convert())
        }
        awsAssertOpSuccess(aws_hash_update(hash, inputCursor)) {
            "aws_hash_update"
        }
    }

    // aws_hash_finalize
    public fun digest(): ByteArray {
        val output: CPointer<aws_byte_buf> = aws_mem_calloc(
            Allocator.Default,
            1.convert(),
            sizeOf<aws_byte_buf>().convert(),
        )?.reinterpret() ?: throw CrtRuntimeException("aws_mem_calloc() aws_byte_buf")
        aws_byte_buf_init(output, Allocator.Default.allocator, 10_000U)

        awsAssertOpSuccess(aws_hash_finalize(hash, output, 0U)) {
            "aws_hash_finalize"
        }

        checkNotNull(output.pointed.buffer) { "expected output buffer to be non-null" }
        return output.pointed.buffer!!.readBytes(output.pointed.len.toInt()).also { reset() }
    }

    // aws_hash_destroy
    public fun reset() {
        aws_hash_destroy(hash)
        hash = initializeHash()
    }

    private fun initializeHash() = checkNotNull(initializeFn(Allocator.Default.allocator)) {
        "failed to initialize hash"
    }
}

/**
 * SHA-1 hash function implemented using bindings to CRT
 */
public class Sha1Native : ShaNative(::aws_sha1_new)

/**
 * SHA-256 hash function implemented using bindings to CRT
 */
public class Sha256Native : ShaNative(::aws_sha256_new)
