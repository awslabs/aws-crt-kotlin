/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt.io
import java.net.URI

internal actual fun parseUri(uri: String): Uri {
    val parsed = URI.create(uri)
    return Uri.build {
        scheme = Protocol.createOrDefault(parsed.scheme)
        host = parsed.host
        port = parsed.port
        path = parsed.path
        if (parsed.query != null && parsed.query.isNotBlank()) parameters = parsed.query
        if (parsed.fragment != null) fragment = parsed.fragment
    }
}
