/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.io.ClientBootstrap
import aws.sdk.kotlin.crt.io.TlsContext

/**
 * Provides credentials from STS by assuming a role
 */
public expect class StsAssumeRoleCredentialsProvider
internal constructor(
    builder: StsAssumeRoleCredentialsProviderBuilder,
) : CredentialsProvider {
    public companion object { }

    override fun close()
    override suspend fun getCredentials(): Credentials
    override suspend fun waitForShutdown()
}

public class StsAssumeRoleCredentialsProviderBuilder {
    /**
     * Connection bootstrap to use for any network connections made while sourcing credentials.
     */
    public var clientBootstrap: ClientBootstrap? = null

    /**
     * The tls context to use for any secure network connections made while sourcing credentials.
     */
    public var tlsContext: TlsContext? = null

    /**
     * The underlying Credentials Provider to use for source credentials.
     */
    public var credentialsProvider: CredentialsProvider? = null

    /**
     * The target role's ARN.
     */
    public var roleArn: String? = null

    /**
     * The name to associate with the session
     */
    public var sessionName: String? = null

    /**
     * The number of seconds from authentication that the session is valid for
     */
    public var durationSeconds: Int? = null

    public fun build(): StsAssumeRoleCredentialsProvider = StsAssumeRoleCredentialsProvider(this)
}

/**
 * Construct a new credentials provider using a builder.
 */
public fun StsAssumeRoleCredentialsProvider.Companion.build(block: StsAssumeRoleCredentialsProviderBuilder.() -> Unit): StsAssumeRoleCredentialsProvider = StsAssumeRoleCredentialsProviderBuilder().apply(block).build()
