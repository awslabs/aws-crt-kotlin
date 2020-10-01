/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import kotlinx.cinterop.*
import kotlinx.cinterop.convert
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
        require(offset >= 0) { "offset must be >= 0" }
        require((dest.size - offset) >= len) { "destination ByteArray is too small" }

        val read = minOf(len, dest.size - offset)
        dest.usePinned {
            println("memcpy(${it.addressOf(offset)}, ${cursor.pointed.ptr}, $read)")
            memcpy(it.addressOf(offset), cursor.pointed.ptr, read.convert())
        }

        return read
    }

    override fun readAll(): ByteArray {
        val buffer = ByteArray(len)
        copyTo(buffer)
        return buffer
    }
}
