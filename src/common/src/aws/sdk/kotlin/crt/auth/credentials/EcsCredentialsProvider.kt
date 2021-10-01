/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.io.ClientBootstrap
import aws.sdk.kotlin.crt.io.TlsContext

/**
 * A credentials provider that sources credentials from an ECS environment.
 */
public expect class EcsCredentialsProvider
internal constructor(builder: EcsCredentialsProviderBuilder) : CredentialsProvider {
    public companion object
}

public class EcsCredentialsProviderBuilder {
    /**
     * Connection bootstrap to use for any network connections made while sourcing credentials.
     */
    public var clientBootstrap: ClientBootstrap? = null

    /**
     * The tls context to use for any secure network connections made while sourcing credentials.
     */
    public var tlsContext: TlsContext? = null

    /**
     * The host component of the URL to query credentials from.
     * Example: www.test.com
     */
    public var host: String? = null

    /**
     * The path and query components of the URI, concatenated, to query credentials from.
     * Example: "/path/to/resource?test1=value1&test%20space=value%20space&test2=value2&test2=value3"
     */
    public var pathAndQuery: String? = null

    /**
     * Provide token to pass to ECS credential service.
     */
    public var authToken: String? = null

    public fun build(): EcsCredentialsProvider = EcsCredentialsProvider(this)
}

/**
 * Construct a new ECS credentials provider using a builder.
 */
public fun EcsCredentialsProvider.Companion.build(block: EcsCredentialsProviderBuilder.() -> Unit):
    EcsCredentialsProvider = EcsCredentialsProviderBuilder().apply(block).build()
