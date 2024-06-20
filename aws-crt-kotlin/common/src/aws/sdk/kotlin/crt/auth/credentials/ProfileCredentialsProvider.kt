/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.io.ClientBootstrap
import aws.sdk.kotlin.crt.io.TlsContext

/**
 * A credentials provider that uses profile files.
 */
public expect class ProfileCredentialsProvider
internal constructor(builder: ProfileCredentialsProviderBuilder) : CredentialsProvider {
    public companion object { }

    override fun close()
    override suspend fun getCredentials(): Credentials
    override suspend fun waitForShutdown()
}

public class ProfileCredentialsProviderBuilder {
    /**
     * Connection bootstrap to use for any network connections made while sourcing credentials.
     */
    public var clientBootstrap: ClientBootstrap? = null

    /**
     * The tls context to use for any secure network connections made while sourcing credentials.
     */
    public var tlsContext: TlsContext? = null

    /**
     * The name of the profile to use (or `"default"` if none is specified).
     */
    public var profileName: String? = null

    /**
     * The name of the config file to use. If none is specified, the default is `".aws/config"` on Linux/Mac and
     * `"%USERPROFILE%\.aws\config"` on Windows.
     */
    public var configFileName: String? = null

    /**
     * The name of the credentials file to use. If none is specified, the default is `".aws/credentials"` on Linux/Mac
     * and `"%USERPROFILE%\.aws\credentials"` on Windows.
     */
    public var credentialsFileName: String? = null

    public fun build(): ProfileCredentialsProvider = ProfileCredentialsProvider(this)
}

/**
 * Construct a new credentials provider using a builder.
 */
public fun ProfileCredentialsProvider.Companion.build(block: ProfileCredentialsProviderBuilder.() -> Unit):
    ProfileCredentialsProvider = ProfileCredentialsProviderBuilder().apply(block).build()
