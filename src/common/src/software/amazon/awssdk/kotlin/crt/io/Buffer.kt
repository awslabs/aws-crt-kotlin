/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

/**
 * Represents an immutable byte buffer that can be read from
 */
public interface Buffer {

    public companion object {
        public val Empty: Buffer = EmptyBuffer
    }

    /**
     * The total number of bytes in the buffer
     */
    public val len: Int

    /**
     * Reads [len] bytes into [dest] and returns the amount read
     * @param dest The array to copy into
     * @param offset The offset into the destination to start copying bytes at
     * @return The number of bytes copied
     * @throws InvalidArgumentException if either the offset is negative or the destination buffer does
     * not have enough capacity
     */
    public fun copyTo(dest: ByteArray, offset: Int = 0): Int

    /**
     * Read the entire buffer into a new [ByteArray] instance
     */
    public fun readAll(): ByteArray
}

/**
 * Represents a mutable byte buffer than can be written to
 */
public interface MutableBuffer : Buffer {
    /**
     * The number of bytes this buffer can currently hold without re-allocating
     */
    public val capacity: Int

    /**
     * Append the entire contents of [src] to the buffer
     */
    public fun write(src: ByteArray): Int
}

private object EmptyBuffer : Buffer {
    override val len: Int = 0
    override fun copyTo(dest: ByteArray, offset: Int): Int { return 0 }
    override fun readAll(): ByteArray = byteArrayOf()
}
