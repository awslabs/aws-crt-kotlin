/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider as CredentialsProviderJni
import software.amazon.awssdk.crt.auth.credentials.StsWebIdentityCredentialsProvider as StsWebIdentityCredentialsProviderJni

public actual class StsWebIdentityCredentialsProvider
internal actual constructor(
    builder: StsWebIdentityCredentialsProviderBuilder,
) : JniCredentialsProvider(),
    CredentialsProvider {
    public actual companion object {}

    override val jniCredentials: CredentialsProviderJni =
        StsWebIdentityCredentialsProviderJni
            .builder()
            .withClientBootstrap(builder.clientBootstrap?.jniBootstrap)
            .withTlsContext(builder.tlsContext?.jniCtx)
            .build()
}
