/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.CrtTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

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
}
