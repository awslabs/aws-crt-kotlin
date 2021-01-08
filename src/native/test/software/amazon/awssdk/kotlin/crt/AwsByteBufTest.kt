/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.*
import libcrt.aws_byte_buf
import software.amazon.awssdk.kotlin.crt.io.AwsByteBuf
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsByteBufTest : CrtTest() {

    @Test
    fun testMutableBuffer() {
        memScoped {
            val buffer = ByteArray(34)
            buffer.usePinned { pinned ->
                val byteBuf = alloc<aws_byte_buf>()
                byteBuf.allocator = Allocator.Default.allocator
                byteBuf.buffer = pinned.addressOf(0).reinterpret()
                byteBuf.capacity = buffer.size.convert()
                byteBuf.len = (0).convert()

                val awsByteBuf = AwsByteBuf(byteBuf.ptr)

                assertEquals(buffer.size.toLong(), awsByteBuf.capacity)
                assertEquals(0, awsByteBuf.len)

                val src = "a tay is a hammer;".encodeToByteArray()
                val written1 = awsByteBuf.write(src)
                assertEquals(src.size, written1)
                assertEquals(src.size, awsByteBuf.len)

                val src2 = " a lep is a ball".encodeToByteArray()
                val written2 = awsByteBuf.write(src2)
                assertEquals(src2.size, written2)
                // should be filled
                assertEquals(awsByteBuf.capacity, awsByteBuf.len.toLong())

                // additional writes should now fail
                val src3 = "bonus points if you know what I'm talking about".encodeToByteArray()
                val written3 = awsByteBuf.write(src3)
                assertEquals(0, written3)
            }

            val actual = buffer.decodeToString()
            assertEquals("a tay is a hammer; a lep is a ball", actual)
        }
    }
}
