/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.CrtTest
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@OptIn(ExperimentalForeignApi::class)
class MutableBufferTest : CrtTest() {
    @Test
    fun testSimpleWrite() {
        val capacity = 10
        val buffer = MutableBuffer(capacity = capacity)
        assertEquals(capacity, buffer.writeRemaining)

        val data = "Hello!"
        buffer.write(data.encodeToByteArray())
        assertEquals(capacity - data.length, buffer.writeRemaining)

        assertContentEquals(data.encodeToByteArray(), buffer.bytes.copyOfRange(0, data.length))

        buffer.close()
    }

    @Test
    fun testWriteFillingBuffer() {
        val capacity = 5
        val buffer = MutableBuffer(capacity = capacity)
        assertEquals(capacity, buffer.writeRemaining)

        val data = "Hello, this data won't fit!"
        assertEquals(5, buffer.write(data.encodeToByteArray()))
        buffer.close()
    }

    @Test
    fun testWriteToFullBuffer() {
        val str = "Hello!"
        val bytes = str.encodeToByteArray()
        val buffer = MutableBuffer.of(bytes) // creates a full buffer

        assertEquals(0, buffer.writeRemaining)

        // since it's full, should write 0 bytes
        assertEquals(0, buffer.write(bytes))
        buffer.close()
    }
}
