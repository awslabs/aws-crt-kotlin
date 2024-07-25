/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.http.HttpProxyOptions
import aws.sdk.kotlin.crt.io.ClientBootstrap
import aws.sdk.kotlin.crt.io.TlsContext

/**
 * Creates a provider that sources credentials from IoT Core
 *
 * @return the newly-constructed credentials provider
 */
public expect class X509CredentialsProvider internal constructor(builder: X509CredentialsProviderBuilder) : CredentialsProvider {
    public companion object { }

    override fun close()
    override suspend fun getCredentials(): Credentials
    override suspend fun waitForShutdown()
}

public class X509CredentialsProviderBuilder {
    /**
     * Connection bootstrap to use for any network connections made while sourcing credentials
     */
    public var clientBootstrap: ClientBootstrap? = null

    /** TLS connection options that have been initialized with your x509 certificate and private key */
    public var tlsContext: TlsContext? = null

    /** IoT thing name you registered with AWS IOT for your device, it will be used in http request header */
    public var thingName: String? = null

    /** Iot role alias you created with AWS IoT for your IAM role, it will be used in http request path */
    public var roleAlias: String? = null

    /**
     * AWS account specific endpoint that can be acquired using AWS CLI following instructions from the giving demo
     * example: c2sakl5huz0afv.credentials.iot.us-east-1.amazonaws.com
     *
     * This a different endpoint than the IoT data mqtt broker endpoint.
     */
    public var endpoint: String? = null

    /**
     * (Optional) Http proxy configuration for the http request that fetches credentials
     */
    public var proxyOptions: HttpProxyOptions? = null

    public fun build(): X509CredentialsProvider {
        requireNotNull(thingName) { "X509CredentialsProvider: thingName must not be null" }
        requireNotNull(roleAlias) { "X509CredentialsProvider: roleAlias must not be null" }
        requireNotNull(endpoint) { "X509CredentialsProvider: endpoint must not be null" }
        requireNotNull(clientBootstrap) { "X509CredentialsProvider: clientBootstrap must not be null" }
        requireNotNull(tlsContext) { "X509CredentialsProvider: tlsContext must not be null" }

        return X509CredentialsProvider(this)
    }
}

/**
 * Construct a new credentials provider using a builder
 */
public fun X509CredentialsProvider.Companion.build(block: X509CredentialsProviderBuilder.() -> Unit): X509CredentialsProvider = X509CredentialsProviderBuilder().apply(block).build()
