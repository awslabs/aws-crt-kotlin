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
 * A typealias to CRT's aws_<checksum_algorithm>_new functions (i.e. aws_sha1_new, aws_sha256_new))
 */
internal typealias InitializeHashFn = (
    allocator: CValuesRef<aws_allocator>?,
) -> CPointer<aws_hash>?

/**
 * SHA-1 hash function implemented using bindings to CRT
 */
public class Sha1 : HashFunction {
    private val sha1 = Sha(::aws_sha1_new)
    override fun update(input: ByteArray, offset: Int, length: Int) { sha1.update(input, offset, length) }
    override fun digest(): ByteArray = sha1.digest()
    override fun reset() { sha1.reset() }
}

/**
 * SHA-256 hash function implemented using bindings to CRT
 */
public class Sha256 : HashFunction {
    private val sha256 = Sha(::aws_sha256_new)
    override fun update(input: ByteArray, offset: Int, length: Int) { sha256.update(input, offset, length) }
    override fun digest(): ByteArray = sha256.digest()
    override fun reset() { sha256.reset() }
}

internal class Sha(val initializeFn: InitializeHashFn) : HashFunction {
    private var hash: CPointer<aws_hash>

    init {
        hash = initializeHash()
    }

    // aws_hash_update
    override fun update(input: ByteArray, offset: Int, length: Int) {
        val inputCursor = input.usePinned {
            aws_byte_cursor_from_array(it.addressOf(offset), length.convert())
        }
        awsAssertOpSuccess(aws_hash_update(hash, inputCursor)) {
            "aws_hash_update"
        }
    }

    // aws_hash_finalize
    override fun digest(): ByteArray {
        val output: CPointer<aws_byte_buf> = aws_mem_calloc(
            Allocator.Default,
            1.convert(),
            sizeOf<aws_byte_buf>().convert(),
        )?.reinterpret() ?: throw CrtRuntimeException("aws_mem_calloc() aws_byte_buf")
        aws_byte_buf_init(output, Allocator.Default.allocator, 32U)

        awsAssertOpSuccess(aws_hash_finalize(hash, output, 0U)) {
            "aws_hash_finalize"
        }

        checkNotNull(output.pointed.buffer) { "expected output buffer to be non-null" }
        return output.pointed.buffer!!.readBytes(output.pointed.len.toInt()).also { reset() }
    }

    // aws_hash_destroy
    override fun reset() {
        aws_hash_destroy(hash)
        hash = initializeHash()
    }

    private fun initializeHash() = checkNotNull(initializeFn(Allocator.Default.allocator)) {
        "failed to initialize hash"
    }
}
