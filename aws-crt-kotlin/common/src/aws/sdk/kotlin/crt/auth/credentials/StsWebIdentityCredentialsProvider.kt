/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.io.ClientBootstrap
import aws.sdk.kotlin.crt.io.TlsContext

/**
 * Sts with web identity credentials provider sources a set of temporary security credentials for users who have been
 * authenticated in a mobile or web application with a web identity provider.
 */
public expect class StsWebIdentityCredentialsProvider
internal constructor(builder: StsWebIdentityCredentialsProviderBuilder) : CredentialsProvider {
    public companion object { }

    override fun close()
    override suspend fun getCredentials(): Credentials
    override suspend fun waitForShutdown()
}

public class StsWebIdentityCredentialsProviderBuilder {
    /**
     * Connection bootstrap to use for any network connections made while sourcing credentials.
     */
    public var clientBootstrap: ClientBootstrap? = null

    /**
     * The tls context to use for any secure network connections made while sourcing credentials.
     */
    public var tlsContext: TlsContext? = null

    public fun build(): StsWebIdentityCredentialsProvider = StsWebIdentityCredentialsProvider(this)
}

/**
 * Construct a new credentials provider using a builder.
 */
public fun StsWebIdentityCredentialsProvider.Companion.build(block: StsWebIdentityCredentialsProviderBuilder.() -> Unit):
    StsWebIdentityCredentialsProvider = StsWebIdentityCredentialsProviderBuilder().apply(block).build()
