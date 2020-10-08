/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import kotlin.test.Test
import kotlin.test.assertEquals

class HttpRequestBuilderTest {
    object TestBodyStream : HttpRequestBodyStream

    @Test
    fun itBuilds() {
        val actual = HttpRequest.build {
            method = "POST"
            encodedPath = "/foo/bar"
            headers {
                append("X-Baz", "quux")
                append("X-Baz", "v2")
            }
            body = TestBodyStream
        }

        assertEquals("POST", actual.method)
        assertEquals("/foo/bar", actual.encodedPath)
        assertEquals("quux", actual.headers["X-Baz"])
        assertEquals(2, actual.headers.getAll("X-Baz")!!.size)
        assertEquals(TestBodyStream, actual.body)
    }
}
