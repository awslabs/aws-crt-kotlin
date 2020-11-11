/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import software.amazon.awssdk.kotlin.crt.runSuspendTest
import software.amazon.awssdk.kotlin.crt.util.Digest
import software.amazon.awssdk.kotlin.crt.util.encodeToHex
import kotlin.test.*

class HttpRequestResponseTest : HttpClientTest() {
    // TODO - port over upload/download tests from crt-java and add async/parallel tests for coroutines...we are
    // currently limited by K/N concurrency model

    private val TEST_DOC_LINE =
        "This is a sample to prove that http downloads and uploads work. It doesn't really matter what's in here, we mainly just need to verify the downloads and uploads work."
    private val TEST_DOC_SHA256 = "c7fdb5314b9742467b16bd5ea2f8012190b5e2c44a005f7984f89aab58219534"

    // no body request
    private suspend fun testSimpleRequest(verb: String, url: String, expectedStatus: Int) {
        try {
            val response = roundTrip(url = url, verb = verb)
            assertEquals(
                expectedStatus,
                response.statusCode,
                "[$url]: expected http status ($expectedStatus) does not match"
            )
        } catch (ex: Exception) {
            fail("[$url]: failed to round trip request: $ex")
        }
    }

    @Test
    fun testHttpGet() = runSuspendTest {
        testSimpleRequest("GET", "https://httpbin.org/get", 200)
        testSimpleRequest("GET", "https://httpbin.org/post", 405)
        testSimpleRequest("GET", "https://httpbin.org/put", 405)
        testSimpleRequest("GET", "https://httpbin.org/delete", 405)
    }

    @Test
    fun testHttpPost() = runSuspendTest {
        testSimpleRequest("POST", "https://httpbin.org/get", 405)
        testSimpleRequest("POST", "https://httpbin.org/post", 200)
        testSimpleRequest("POST", "https://httpbin.org/put", 405)
        testSimpleRequest("POST", "https://httpbin.org/delete", 405)
    }

    @Test
    fun testHttpPut() = runSuspendTest {
        testSimpleRequest("PUT", "https://httpbin.org/get", 405)
        testSimpleRequest("PUT", "https://httpbin.org/post", 405)
        testSimpleRequest("PUT", "https://httpbin.org/put", 200)
        testSimpleRequest("PUT", "https://httpbin.org/delete", 405)
    }

    @Test
    fun testHttpDelete() = runSuspendTest {
        testSimpleRequest("DELETE", "https://httpbin.org/get", 405)
        testSimpleRequest("DELETE", "https://httpbin.org/post", 405)
        testSimpleRequest("DELETE", "https://httpbin.org/put", 405)
        testSimpleRequest("DELETE", "https://httpbin.org/delete", 200)
    }

    @Test
    fun testHttpDownload() = runSuspendTest {
        val response = roundTrip(url = "https://aws-crt-test-stuff.s3.amazonaws.com/http_test_doc.txt", verb = "GET")
        assertEquals(200, response.statusCode, "expected http status does not match")
        assertNotNull(response.body)

        assertEquals(TEST_DOC_SHA256, Digest.sha256(response.body).encodeToHex())
    }

    @Test
    fun testHttpUpload() = runSuspendTest {
        val bodyToSend = TEST_DOC_LINE
        val response = roundTrip(url = "https://httpbin.org/anything", verb = "PUT", body = bodyToSend)
        assertEquals(200, response.statusCode, "expected http status does not match")
        assertNotNull(response.body, "expected a response body for http upload")

        /**
         * Example Json Response Body from httpbin.org:
         *
         * {
         *   "args": {},
         *   "data": "This is a sample to prove that http downloads and uploads work. It doesn't really matter what's in here, we mainly just need to verify the downloads and uploads work.",
         *   "files": {},
         *   "form": {},
         *   "headers": {
         *     "Content-Length": "166",
         *     "Host": "httpbin.org"
         *   },
         *   "json": null,
         *   "method": "PUT",
         *   "origin": "1.2.3.4, 5.6.7.8",
         *   "url": "https://httpbin.org/anything"
         * }
         */
        val bodyText = response.body.decodeToString().split("\n")
        var echoedBody: String? = null
        for (line in bodyText) {
            val keyAndValue = line.split(":", limit = 2)
            if (keyAndValue.size == 2) {
                val key = extractValueFromJson(keyAndValue[0])
                val value = extractValueFromJson(keyAndValue[1])

                if (key == "data") {
                    echoedBody = extractValueFromJson(value)
                }
            }
        }
        assertNotNull(echoedBody, "body not found from echoed response")
        assertEquals(bodyToSend, echoedBody)
    }

    private fun extractValueFromJson(input: String): String {
        return input.trim()
            .replace(Regex(",$"), "") // remove comma if it's the last character
            .replace(Regex("^\"|\"$"), "") // strip quotes
    }
}
