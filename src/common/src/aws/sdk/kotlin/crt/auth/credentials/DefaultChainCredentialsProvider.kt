/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.io.ClientBootstrap

/**
 * Creates the default provider chain used by most AWS SDKs.
 *
 * Generally:
 *
 * (1) Environment
 * (2) Profile
 * (3) (conditional, off by default) ECS
 * (4) (conditional, on by default) EC2 Instance Metadata
 *
 * Support for environmental control of the default provider chain is not yet
 * implemented.
 *
 * @return the newly-constructed credentials provider
 */
public expect class DefaultChainCredentialsProvider internal constructor(builder: DefaultChainCredentialsProviderBuilder) :
    CredentialsProvider {

    public companion object
}

public class DefaultChainCredentialsProviderBuilder {
    public var clientBootstrap: ClientBootstrap? = null

    public fun build(): DefaultChainCredentialsProvider {
        requireNotNull(clientBootstrap) { "DefaultChainCredentialsProvider: clientBootstrap must be provided" }
        return DefaultChainCredentialsProvider(this)
    }
}

/**
 * Construct a new credentials provider using a builder
 */
public fun DefaultChainCredentialsProvider.Companion.build(block: DefaultChainCredentialsProviderBuilder.() -> Unit):
    DefaultChainCredentialsProvider = DefaultChainCredentialsProviderBuilder().apply(block).build()
