/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.http

import aws.sdk.kotlin.crt.io.MutableBuffer

/**
 * Interface that CRT knows how to call to request an outgoing request payload body
 */
public interface HttpRequestBodyStream {
    public companion object {
        /**
         * Create a new [HttpRequestBodyStream] using the underlying [ByteArray] as the stream contents to send
         */
        public fun fromByteArray(src: ByteArray): HttpRequestBodyStream = ByteArrayBodyStream(src)
    }

    /**
     * Called from CRT when the Http request has a body.
     *
     * Note this function may be called many times
     *
     * @param buffer The outgoing buffer to write the payload to.
     * @return true if the body has been completely written, false otherwise
     */
    public fun sendRequestBody(buffer: MutableBuffer): Boolean = true

    /**
     * Called when the processing needs the stream to rewind itself back to the beginning.
     * If the stream does not support rewinding or the rewind fails, false should be returned
     *
     * Payload signing requires a rewindable stream, basic HTTP does not
     *
     * @return true if the stream was successfully rewound, false otherwise
     */
    public fun resetPosition(): Boolean = false
}

private class ByteArrayBodyStream(val src: ByteArray) : HttpRequestBodyStream {
    private var currPos: Int = 0

    override fun sendRequestBody(buffer: MutableBuffer): Boolean {
        currPos += buffer.write(src, currPos)
        return currPos == src.size
    }

    override fun resetPosition(): Boolean {
        currPos = 0
        return true
    }
}
