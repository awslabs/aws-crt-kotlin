/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.crt.util.encodeToHex
import kotlin.test.Test
import kotlin.test.assertEquals

class ShaNativeTest {
    @Test
    fun testUpdate() {
        // algorithm -> listOf(hash("uh"), hash(""))
        val tests = listOf(
            (Sha1Native() to listOf("80c3d7b3f509a5ac8bb29d7f8c4a94af14f7d244", "da39a3ee5e6b4b0d3255bfef95601890afd80709")),
            (Sha256Native() to listOf("beba745afae8503925089cc2f3cc9b87e849e81c07531e83c5c341a63bcaaed9", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")),
        )

        tests.forEach { (hash, expected) ->
            val data = "uh".encodeToByteArray()
            hash.update(data, 0, data.size)

            // hash of "uh"
            assertEquals(expected[0], hash.digest().encodeToHex())

            // hash of an empty string
            assertEquals(expected[1], hash.digest().encodeToHex())
        }
    }

    @Test
    fun testMultipleUpdatesWithOffset() {
        // algorithm -> hash("uh") + hash("huh")
        val tests = listOf(
            (Sha1Native() to "c093098e7a1a8ec547e82a399d63f331760e9a57"),
            (Sha256Native() to "b474fb57f69c1b5aa00aa27ad0b36c03fe9915cc497344d6a55a86b2e9bd1b72"),
        )

        tests.forEach { (hash, expected) ->
            val data = "ahuhi".encodeToByteArray()
            hash.update(data, 2, 2) // uh

            hash.update(data, 1, 3) // huh

            assertEquals(expected, hash.digest().encodeToHex())
        }
    }
}
