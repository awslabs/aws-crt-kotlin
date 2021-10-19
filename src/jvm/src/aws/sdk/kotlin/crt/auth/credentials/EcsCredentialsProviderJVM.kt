/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.auth.credentials

import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider as CredentialsProviderJni
import software.amazon.awssdk.crt.auth.credentials.EcsCredentialsProvider as EcsCredentialsProviderJni

public actual class EcsCredentialsProvider
internal actual constructor(builder: EcsCredentialsProviderBuilder) :
    CredentialsProvider, JniCredentialsProvider() {
    public actual companion object {}

    override val jniCredentials: CredentialsProviderJni =
        EcsCredentialsProviderJni
            .builder()
            .withClientBootstrap(builder.clientBootstrap?.jniBootstrap)
            .withTlsContext(builder.tlsContext?.jniCtx)
            .withHost(builder.host)
            .withPathAndQuery(builder.pathAndQuery)
            .withAuthToken(builder.authToken)
            .build()
}
