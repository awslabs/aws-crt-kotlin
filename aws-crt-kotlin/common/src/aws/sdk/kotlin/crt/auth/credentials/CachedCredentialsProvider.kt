/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

/**
 * Creates a provider that functions as a caching decorating of another provider.
 *
 * Credentials sourced through this provider will be cached within it until their expiration time.
 * When the cached credentials expire, new credentials will be fetched when next queried.
 *
 * For example, the default chain is implemented as:
 *
 * CachedProvider -> ProviderChain(EnvironmentProvider -> ProfileProvider -> ECS/EC2IMD etc...)
 *
 * @return the newly-constructed credentials provider
 */
public expect class CachedCredentialsProvider internal constructor(builder: CachedCredentialsProviderBuilder) : CredentialsProvider {
    public companion object
}

public class CachedCredentialsProviderBuilder {
    /**
     * The provider to cache credentials query results from
     */
    public var source: CredentialsProvider? = null

    /**
     * An optional expiration time period for sourced credentials.  For a given set of cached credentials,
     * the refresh time period will be the minimum of this time and any expiration timestamp on the credentials
     * themselves.
     */
    public var refreshTimeInMilliseconds: Long = 0

    public fun build(): CachedCredentialsProvider {
        requireNotNull(source) { "CachedCredentialsProvider requires a source provider to wrap" }
        return CachedCredentialsProvider(this)
    }
}

/**
 * Construct a new credentials provider using a builder
 */
public fun CachedCredentialsProvider.Companion.build(block: CachedCredentialsProviderBuilder.() -> Unit):
    CachedCredentialsProvider = CachedCredentialsProviderBuilder().apply(block).build()
