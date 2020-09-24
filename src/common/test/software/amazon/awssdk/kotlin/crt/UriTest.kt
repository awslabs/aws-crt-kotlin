/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import software.amazon.awssdk.kotlin.crt.http.Uri
import software.amazon.awssdk.kotlin.crt.http.UriBuilder
import software.amazon.awssdk.kotlin.crt.http.UserInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class UriTest {
    @Test
    fun basicToString() {
        val expected = "https://test.aws.com/kotlin"
        val uri = Uri(
            "https",
            "test.aws.com",
            path = "/kotlin"
        )
        assertEquals(expected, uri.toString())
    }

    @Test
    fun forceRetainQuery() {
        val expected = "https://test.aws.com/kotlin?"
        val uri = UriBuilder.build {
            host = "test.aws.com"
            path = "/kotlin"
            forceQuery = true
        }
        assertEquals(expected, uri.toString())
    }

    @Test
    fun withParameters() {
        val expected = "https://test.aws.com/kotlin?foo=bar&baz=quux&baz=qux"

        val uri = Uri(
            "https",
            "test.aws.com",
            path = "/kotlin",
            parameters = "foo=bar&baz=quux&baz=qux"
        )
        assertEquals(expected, uri.toString())
    }

    @Test
    fun specificPort() {
        val expected = "https://test.aws.com:8000"
        val uri = Uri(
            "https",
            "test.aws.com",
            port = 8000
        )
        assertEquals(expected, uri.toString())

        val expected2 = "http://test.aws.com"
        val uri2 = Uri(
            "http",
            "test.aws.com",
            port = 80
        )
        assertEquals(expected2, uri2.toString())
    }

    @Test
    fun portRange() {
        fun checkPort(n: Int) {
            assertEquals(
                n,
                Uri(
                    "https",
                    "test.aws.com",
                    port = n
                ).port
            )
        }

        checkPort(1)
        checkPort(65536)
        assertFails {
            checkPort(65537)
        }
    }

    @Test
    fun userinfoNoPassword() {
        val expected = "https://user@test.aws.com"
        val uri = UriBuilder.build {
            scheme = "https"
            host = "test.aws.com"
            userInfo = UserInfo("user", "")
        }
        assertEquals(expected, uri.toString())
    }

    @Test
    fun fullUserinfo() {
        val expected = "https://user:password@test.aws.com"
        val uri = UriBuilder.build {
            scheme = "https"
            host = "test.aws.com"
            userInfo = UserInfo("user", "password")
        }
        assertEquals(expected, uri.toString())
    }

    @Test
    fun itBuilds() {
        val builder = UriBuilder()
        builder.scheme = "http"
        builder.host = "test.aws.com"
        builder.path = "/kotlin"
        val uri = builder.build()
        val expected = "http://test.aws.com/kotlin"
        assertEquals(expected, uri.toString())
        assertEquals("http", builder.scheme)
        assertEquals("test.aws.com", builder.host)
        assertEquals(null, builder.port)
        assertEquals(null, builder.fragment)
        assertEquals(null, builder.userInfo)
    }

    @Test
    fun itBuildsWithNonDefaultPort() {
        val uri = UriBuilder.build {
            scheme = "http"
            host = "test.aws.com"
            path = "/kotlin"
            port = 3000
        }
        val expected = "http://test.aws.com:3000/kotlin"
        assertEquals(expected, uri.toString())
    }

    @Test
    fun itBuildsWithParameters() {
        val uri = UriBuilder.build {
            scheme = "http"
            host = "test.aws.com"
            path = "/kotlin"
            parameters = "foo=baz"
        }
        val expected = "http://test.aws.com/kotlin?foo=baz"
        assertEquals(expected, uri.toString())
    }
}
