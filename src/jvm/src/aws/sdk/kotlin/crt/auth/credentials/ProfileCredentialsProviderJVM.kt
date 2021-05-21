/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.auth.credentials

import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider as CredentialsProviderJni
import software.amazon.awssdk.crt.auth.credentials.ProfileCredentialsProvider as ProfileCredentialsProviderJni

public actual class ProfileCredentialsProvider
internal actual constructor(builder: ProfileCredentialsProviderBuilder) :
    CredentialsProvider, JniCredentialsProvider() {
    public actual companion object {}

    override val jniCredentials: CredentialsProviderJni =
        ProfileCredentialsProviderJni.ProfileCredentialsProviderBuilder().apply {
            withClientBootstrap(builder.clientBootstrap?.jniBootstrap)
            withTlsContext(builder.tlsContext?.jniCtx)
            withProfileName(builder.profileName)
            withConfigFileNameOverride(builder.configFileNameOverride)
            withCredentialsFileNameOverride(builder.credentialsFileNameOverride)
        }.build()
}
