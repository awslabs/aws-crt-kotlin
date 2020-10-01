/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import kotlinx.cinterop.*
import kotlinx.cinterop.convert
import libcrt.AWS_OP_SUCCESS
import libcrt.aws_byte_buf
import libcrt.aws_byte_buf_append
import libcrt.aws_byte_cursor
import platform.posix.memcpy

/**
 * A [Buffer] instance backed by an aws_byte_cursor
 */
internal class ByteCursorBuffer(
    private val cursor: CPointer<aws_byte_cursor>
) : Buffer {

    override val len: Int
        get() = cursor.pointed.len.convert()

    override fun copyTo(dest: ByteArray, offset: Int): Int {
        val ptr = cursor.pointed.ptr
        requireNotNull(ptr) { "cursor buffer cannot be null" }
        return copyTo(dest, ptr, len, offset)
    }

    override fun readAll(): ByteArray {
        val buffer = ByteArray(len)
        copyTo(buffer)
        return buffer
    }
}

/**
 * Wrap an aws_byte_buf instance as a [MutableBuffer]
 */
internal class AwsByteBuf(private val buf: CPointer<aws_byte_buf>) : MutableBuffer {
    override val capacity: Long
        get() = buf.pointed.capacity.convert()

    override val len: Int
        get() = buf.pointed.len.convert()

    override fun write(src: ByteArray): Int {
        // copy as much of src as we can
        return src.usePinned { pinned ->
            val remaining: Int = (capacity - len.toLong()).toInt()
            val writeSize = minOf(remaining, src.size)
            val cursor = cValue<aws_byte_cursor> {
                len = writeSize.convert()
                ptr = pinned.addressOf(0).reinterpret()
            }

            when (aws_byte_buf_append(buf, cursor)) {
                AWS_OP_SUCCESS -> writeSize
                else -> 0
            }
        }
    }

    override fun copyTo(dest: ByteArray, offset: Int): Int {
        val ptr = buf.pointed.buffer
        requireNotNull(ptr) { "aws_byte_buf.buffer cannot be null" }
        return copyTo(dest, ptr, len, offset)
    }

    override fun readAll(): ByteArray {
        return buf.pointed.buffer?.readBytes(len) ?: byteArrayOf()
    }
}

private fun copyTo(dest: ByteArray, src: CValuesRef<*>, len: Int, offset: Int): Int {
    require(offset >= 0) { "offset must be >= 0" }
    require((dest.size - offset) >= len) { "destination ByteArray is too small" }

    val read = minOf(len, dest.size - offset)
    dest.usePinned {
        memcpy(it.addressOf(offset), src, read.convert())
    }

    return read
}
