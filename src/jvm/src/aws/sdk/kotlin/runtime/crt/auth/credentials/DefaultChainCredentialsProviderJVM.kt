/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt.auth.credentials

import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider as CredentialsProviderJni
import software.amazon.awssdk.crt.auth.credentials.DefaultChainCredentialsProvider as DefaultChainCredentialsProviderJni

public actual class DefaultChainCredentialsProvider internal actual constructor(builder: DefaultChainCredentialsProviderBuilder) :
    CredentialsProvider, JniCredentialsProvider() {
    public actual companion object {}

    override val jniCredentials: CredentialsProviderJni =
        DefaultChainCredentialsProviderJni.DefaultChainCredentialsProviderBuilder().apply {
            withClientBootstrap(builder.clientBootstrap!!.jniBootstrap)
        }.build()
}
