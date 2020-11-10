/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import software.amazon.awssdk.kotlin.crt.runSuspendTest
import kotlin.test.*

class HttpRequestResponseTest : HttpClientTest() {
    // TODO - port over upload/download tests from crt-java and add async/parallel tests for coroutines...we are
    // currently limited by K/N concurrency model

    // no body request
    private suspend fun testSimpleRequest(verb: String, url: String, expectedBody: String?, expectedStatus: Int) {
        try {
            val response = roundTrip(url = url, verb = verb)
            assertEquals(expectedStatus, response.statusCode, "[$url]: expected http status ($expectedStatus) does not match")

            if (expectedBody == null) {
                assertNull(response.body, "[$url]: expected no response body but received: ${response.body?.decodeToString()}")
            } else {
                val actualBody = assertNotNull(response.body, "[$url]: expected a body but did not receive one")
                assertEquals(expectedBody, actualBody.decodeToString(), "[$url]: bodies differ")
            }
        } catch (ex: Exception) {
            fail("[$url]: failed to round trip request: $ex")
        }
    }

    @Test
    fun testHttpGet() = runSuspendTest {
        testSimpleRequest("GET", "https://httpbin.org/get", null, 200)
        testSimpleRequest("GET", "https://httpbin.org/post", null, 405)
        testSimpleRequest("GET", "https://httpbin.org/put", null, 405)
        testSimpleRequest("GET", "https://httpbin.org/delete", null, 405)
    }

    @Test
    fun testHttpPost() = runSuspendTest {
        testSimpleRequest("POST", "https://httpbin.org/get", null, 405)
        testSimpleRequest("POST", "https://httpbin.org/post", null, 200)
        testSimpleRequest("POST", "https://httpbin.org/put", null, 405)
        testSimpleRequest("POST", "https://httpbin.org/delete", null, 405)
    }

    @Test
    fun testHttpPut() = runSuspendTest {
        testSimpleRequest("PUT", "https://httpbin.org/get", null, 405)
        testSimpleRequest("PUT", "https://httpbin.org/post", null, 405)
        testSimpleRequest("PUT", "https://httpbin.org/put", null, 200)
        testSimpleRequest("PUT", "https://httpbin.org/delete", null, 405)
    }

    @Test
    fun testHttpDelete() = runSuspendTest {
        testSimpleRequest("DELETE", "https://httpbin.org/get", null, 405)
        testSimpleRequest("DELETE", "https://httpbin.org/post", null, 405)
        testSimpleRequest("DELETE", "https://httpbin.org/put", null, 405)
        testSimpleRequest("DELETE", "https://httpbin.org/delete", null, 200)
    }
}
