/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

public const val DEFAULT_SCHEME_PORT: Int = -1
private const val DEFAULT_HTTP_PORT: Int = 80
private const val DEFAULT_HTTPS_PORT: Int = 443

/**
 * Represents wire protocol (scheme) used by URI
 *
 * @property name The name (scheme) of the protocol
 * @property defaultPort The default port for the protocol
 */
public data class Protocol(val name: String, val defaultPort: Int) {

    public companion object {
        public val HTTP: Protocol = Protocol("http", DEFAULT_HTTP_PORT)
        public val HTTPS: Protocol = Protocol("https", DEFAULT_HTTPS_PORT)
        public val WS: Protocol = Protocol("ws", DEFAULT_HTTP_PORT)
        public val WSS: Protocol = Protocol("wss", DEFAULT_HTTPS_PORT)

        public val byName: Map<String, Protocol> = listOf(HTTP, HTTPS, WS, WSS).associateBy { it.name }

        public fun createOrDefault(name: String): Protocol = name.toLowerCase().let {
            byName[it] ?: Protocol(it, DEFAULT_SCHEME_PORT)
        }
    }
}

/**
 * Represents an immutable URI of the form: `[scheme:][//[userinfo@]host][/]path[?query][#fragment]`
 *
 * @property scheme The wire protocol (e.g. http, https, ws, wss, etc)
 * @property host hostname
 * @property specifiedPort port to connect to the host on, defaults to the scheme default prot
 * @property path (raw) path without the query
 * @property parameters (raw) query parameters
 * @property fragment URL fragment
 * @property userInfo username and password (optional)
 * @property forceQuery keep trailing question mark regardless of whether there are any query parameters
 */
public data class Uri(
    val scheme: Protocol,
    val host: String,
    val specifiedPort: Int = DEFAULT_SCHEME_PORT,
    val path: String = "",
    val parameters: String? = null,
    val fragment: String? = null,
    val userInfo: UserInfo? = null,
    val forceQuery: Boolean = false
) {

    init {
        if (specifiedPort != DEFAULT_SCHEME_PORT) {
            require(specifiedPort in 1..65536) { "port must be in between 1 and 65536; found: $specifiedPort" }
        }
    }

    /**
     * The actual port to use
     */
    val port: Int = specifiedPort.takeUnless { it == DEFAULT_SCHEME_PORT } ?: scheme.defaultPort

    public companion object {
        /**
         * Build a URI
         */
        public fun build(block: UriBuilder.() -> Unit): Uri = UriBuilder().apply(block).build()

        /**
         * Parse a URI from a string into it's component parts
         */
        public fun parse(uri: String): Uri = parseUri(uri)
    }

    override fun toString(): String = buildString {
        append(scheme.name)
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
        if (specifiedPort != DEFAULT_SCHEME_PORT && specifiedPort != scheme.defaultPort) {
            append(":$specifiedPort")
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
    public var scheme: Protocol = Protocol.HTTPS
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

/**
 * Test if the protocol requires TLS support
 */
public fun Protocol.requiresTls(): Boolean = name == "https" || name == "wss"

/**
 * Test if the protocol is HTTP(S)
 */
internal fun Protocol.isHttp(): Boolean = name == "http" || name == "https"

// platform parse function
internal expect fun parseUri(uri: String): Uri
