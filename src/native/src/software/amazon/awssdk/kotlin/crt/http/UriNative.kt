/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import kotlinx.cinterop.*
import libcrt.aws_uri
import libcrt.aws_uri_clean_up
import libcrt.aws_uri_init_parse
import software.amazon.awssdk.kotlin.crt.Allocator
import software.amazon.awssdk.kotlin.crt.asAwsByteCursor
import software.amazon.awssdk.kotlin.crt.awsAssertOp
import software.amazon.awssdk.kotlin.crt.toKString

@OptIn(ExperimentalUnsignedTypes::class)
internal actual fun parseUri(uri: String): Uri {
    return memScoped {
        uri.encodeToByteArray().usePinned { pinned ->
            val uriCursor = pinned.asAwsByteCursor()
            val awsUri = alloc<aws_uri>()
            awsAssertOp(
                aws_uri_init_parse(awsUri.ptr, Allocator.Default, uriCursor)
            ) { "aws_uri_init_parse() with uri: $uri" }

            Uri.build {
                scheme = Protocol.createOrDefault(awsUri.scheme.toKString())
                host = awsUri.host_name.toKString()
                val parsedPort = awsUri.port.toInt()
                if (parsedPort > 0) { port = parsedPort }
                path = awsUri.path.toKString()

                if (awsUri.query_string.len.toInt() > 0) {
                    parameters = awsUri.query_string.toKString()
                }
                if (awsUri.authority.len.toInt() > 0) {
                    // username will hold both username and password if specified
                    userInfo = UserInfo(awsUri.authority.toKString(), "")
                }

                aws_uri_clean_up(awsUri.ptr)
            }
        }
    }
}
