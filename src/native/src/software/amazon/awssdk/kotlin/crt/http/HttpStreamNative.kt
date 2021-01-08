/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import kotlinx.cinterop.*
import libcrt.*
import software.amazon.awssdk.kotlin.crt.CrtResource
import software.amazon.awssdk.kotlin.crt.CrtRuntimeException

internal class HttpStreamNative(private val stream: CPointer<aws_http_stream>) : HttpStream, CrtResource<aws_http_stream>() {

    override val ptr: CPointer<aws_http_stream> = stream

    override val responseStatusCode: Int
        get() {
            return memScoped {
                val status = alloc<IntVar>()
                if (aws_http_stream_get_incoming_response_status(stream, status.ptr) != AWS_OP_SUCCESS) {
                    throw CrtRuntimeException("error getting response status code from HttpStream")
                }
                status.value
            }
        }

    override fun incrementWindow(size: Int) {
        aws_http_stream_update_window(stream, size.convert())
    }

    override fun activate() {
        if (aws_http_stream_activate(stream) != AWS_OP_SUCCESS) {
            throw CrtRuntimeException("HttpStream activate failed")
        }
    }

    override fun close() {
        aws_http_stream_release(stream)
    }
}
