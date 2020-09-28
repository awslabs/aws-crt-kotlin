/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import kotlinx.cinterop.CPointer
import libcrt.aws_http_connection
import software.amazon.awssdk.kotlin.crt.Closeable
import software.amazon.awssdk.kotlin.crt.CrtResource

internal class HttpClientConnectionNative(
    private val manager: HttpClientConnectionManager,
    private val connection: CPointer<aws_http_connection>
) : HttpClientConnection, Closeable, CrtResource<aws_http_connection>() {

    override val ptr: CPointer<aws_http_connection> = connection

    override suspend fun close() {
        TODO("Not yet implemented")
    }
}
