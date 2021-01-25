/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.http

import aws.sdk.kotlin.crt.io.TlsContext

/**
 * what kind of authentication, if any, to use when connecting to a proxy server
 */
public enum class HttpProxyAuthorizationType(public val value: Int) {
    /**
     * No authentication
     */
    None(0),

    /**
     * Basic (username and password base64 encoded) authentication
     */
    Basic(1);
}

/**
 * This class provides access to Http proxy configuration options
 */
public data class HttpProxyOptions(
    val host: String,
    val port: Int? = null,
    val authUsername: String? = null,
    val authPassword: String? = null,
    val tlsContext: TlsContext? = null,
    val authType: HttpProxyAuthorizationType = HttpProxyAuthorizationType.None
)
