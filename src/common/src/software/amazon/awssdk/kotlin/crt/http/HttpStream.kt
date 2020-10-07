/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import software.amazon.awssdk.kotlin.crt.Closeable

/**
 * An HttpStream represents a single Http Request/Response pair within a HttpClientConnection, and wraps the native resources
 * from the aws-c-http library.
 *
 * Can be used to update the Window size, or to abort the stream early in the middle of sending/receiving Http Bodies.
 */
public interface HttpStream : Closeable {

    /**
     * Retrieve the Http response status code. Available ONLY after the first set of response
     * headers have been received. See [HttpStreamResponseHandler]
     */
    public val responseStatusCode: Int

    /**
     * Opens the Sliding Read/Write Window by the number of bytes passed as an argument for this HttpStream.
     *
     * This function should only be called if the user application previously returned less than the length of the input
     * Buffer from a onResponseBody() call in a HttpStreamResponseHandler, and should be <= to the total number of
     * un-packed bytes.
     *
     * @param size How many bytes to increment the sliding window by.
     */
    public fun incrementWindow(size: Int)

    /**
     * Activate the client stream and start processing the request
     */
    public fun activate()
}
