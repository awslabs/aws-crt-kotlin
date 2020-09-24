/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

public const val DEFAULT_SCHEME_PORT: Int = -1

/**
 * Represents an immutable URI of the form: `[scheme:][//[userinfo@]host][/]path[?query][#fragment]`
 *
 * @property scheme The wire protocol (e.g. http, https, ws, wss, etc)
 * @property host hostname
 * @property port port to connect to the host on, defaults to the scheme default prot
 * @property path (raw) path without the query
 * @property parameters (raw) query parameters
 * @property fragment URL fragment
 * @property userInfo username and password (optional)
 * @property forceQuery keep trailing question mark regardless of whether there are any query parameters
 */
public data class Uri(
    val scheme: String,
    val host: String,
    val port: Int = DEFAULT_SCHEME_PORT,
    val path: String = "",
    val parameters: String? = null,
    val fragment: String? = null,
    val userInfo: UserInfo? = null,
    val forceQuery: Boolean = false
) {
    init {
        if (port != DEFAULT_SCHEME_PORT) {
            require(port in 1..65536) { "port must be in between 1 and 65536" }
        }
    }

    override fun toString(): String = buildString {
        // FIXME - the userinfo, path, and fragment are raw at this point and need escaped as well probably
        append(scheme)
        append("://")
        userInfo?.let { userinfo ->
            if (userinfo.username.isNotBlank()) {
                append(userinfo.username)
                if (userinfo.password.isNotBlank()) {
                    append(":${userinfo.password}")
                }
                append("@")
            }
        }

        append(host)
        if (port != DEFAULT_SCHEME_PORT && !port.isDefaultForScheme(scheme)) {
            append(":$port")
        }

        if (path.isNotBlank()) {
            append("/")
            append(path.removePrefix("/"))
        }

        if ((parameters != null && parameters.isNotEmpty()) || forceQuery) {
            append("?")
        }
        parameters?.let {
            append(it)
        }

        if (fragment != null && fragment.isNotBlank()) {
            append("#")
            append(fragment)
        }
    }
}

/**
 * URL username and password
 */
public data class UserInfo(val username: String, val password: String)

/**
 * Construct a URI by it's individual components
 */
public class UriBuilder {
    public var scheme: String = "https"
    public var host: String = ""
    public var port: Int? = null
    public var path: String = ""
    public var parameters: String? = null
    public var fragment: String? = null
    public var userInfo: UserInfo? = null
    public var forceQuery: Boolean = false

    public companion object {
        public fun build(block: UriBuilder.() -> Unit): Uri = UriBuilder().apply(block).build()
    }

    internal fun build(): Uri = Uri(
        scheme,
        host,
        port ?: DEFAULT_SCHEME_PORT,
        path,
        parameters,
        fragment,
        userInfo,
        forceQuery
    )
}

// most commonly used schemes we provide check for default ports
private fun Int.isDefaultForScheme(scheme: String): Boolean = when (scheme) {
    "http", "ws" -> this == 80
    "https", "wss" -> this == 443
    else -> false
}
