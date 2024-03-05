/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.Closeable
import kotlinx.cinterop.*
import libcrt.*

/**
 * Represents a mutable linear range of bytes that can be written to.
 * Instance of this class has no additional state except the bytes themselves.
 */
@OptIn(ExperimentalForeignApi::class)
public actual class MutableBuffer(private val buffer: aws_byte_buf? = null, private val capacity: Int) : Closeable { // TODO implement CrtResource?
    private val buf = buffer ?: Allocator.Default.alloc<aws_byte_buf>()

    public val bytes: ByteArray
        get() = buf.buffer!!.readBytes(buf.capacity.toInt())

    init {
        if (buffer == null) {
            aws_byte_buf_init(buf = buf.ptr, allocator = Allocator.Default.allocator, capacity = capacity.toULong())
        }
    }

    /**
     * The amount of remaining write capacity before the buffer is full
     */
    public actual val writeRemaining: Int
        get() = buf.capacity.toInt() - buf.len.toInt()

    /**
     * Write as much of [length] bytes from [src] as possible starting at [offset].
     * The number of bytes written is returned which may be less than [length]
     */
    public actual fun write(src: ByteArray, offset: Int, length: Int): Int {
        src.usePinned { pinnedSrc ->
            val offsetPinnedSrc = pinnedSrc.addressOf(offset).reinterpret<UByteVar>()
            val numBytesToWrite = minOf(length, writeRemaining)
            return if (aws_byte_buf_write(buf.ptr, offsetPinnedSrc, numBytesToWrite.toULong())) numBytesToWrite else 0
        }
    }

    public override fun close() {
        if (buffer == null) {
            aws_byte_buf_clean_up(buf.ptr)
        }
    }

    public actual companion object {
        /**
         * Create a buffer instance backed by [src]
         */
        public actual fun of(src: ByteArray): MutableBuffer = src.usePinned { pinnedSrc ->
            val tempBuf: CValue<aws_byte_buf> = aws_byte_buf_from_array(pinnedSrc.addressOf(0), src.size.toULong())

            val buf = Allocator.Default.alloc<aws_byte_buf>()
            // initialize the buf->buffer
            aws_byte_buf_init_copy(dest = buf.ptr, allocator = Allocator.Default.allocator, src = tempBuf)

            MutableBuffer(buf, buf.capacity.toInt())
        }
    }
}
