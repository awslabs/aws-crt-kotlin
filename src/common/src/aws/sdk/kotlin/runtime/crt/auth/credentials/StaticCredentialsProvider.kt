/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt.auth.credentials

/**
 * A credentials provider for a fixed set of credentials
 */
public expect class StaticCredentialsProvider internal constructor(builder: StaticCredentialsProviderBuilder) : CredentialsProvider {
    public companion object
}

public class StaticCredentialsProviderBuilder {
    public var accessKeyId: String? = null
    public var secretAccessKey: String? = null
    public var sessionToken: String? = null

    public fun build(): StaticCredentialsProvider {
        if (accessKeyId == null || secretAccessKey == null) {
            throw IllegalArgumentException("StaticCredentialsProvider - accessKeyId and secretAccessKey must not be null")
        }
        return StaticCredentialsProvider(this)
    }
}

/**
 * Construct a new credentials provider using a builder
 */
public fun StaticCredentialsProvider.Companion.build(block: StaticCredentialsProviderBuilder.() -> Unit):
    StaticCredentialsProvider = StaticCredentialsProviderBuilder().apply(block).build()

/**
 * Create provider from a set of credentials
 */
public fun StaticCredentialsProvider.Companion.fromCredentials(
    credentials: Credentials
): StaticCredentialsProvider = build {
    accessKeyId = credentials.accessKeyId
    secretAccessKey = credentials.secretAccessKey
    sessionToken = credentials.sessionToken
}
