/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials
import aws.sdk.kotlin.crt.http.into
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider as CredentialsProviderJni
import software.amazon.awssdk.crt.auth.credentials.X509CredentialsProvider as X509CredentialsProviderJni

public actual class X509CredentialsProvider internal actual constructor(builder: X509CredentialsProviderBuilder) :
    JniCredentialsProvider(),
    CredentialsProvider {

    public actual companion object {}

    override val jniCredentials: CredentialsProviderJni =
        X509CredentialsProviderJni.X509CredentialsProviderBuilder().apply {
            // validated in builder.build()
            withEndpoint(builder.endpoint!!)
            withRoleAlias(builder.roleAlias!!)
            withThingName(builder.thingName!!)
            withClientBootstrap(builder.clientBootstrap!!.jniBootstrap)
            withTlsContext(builder.tlsContext!!.jniCtx)

            builder.proxyOptions?.let {
                withProxyOptions(it.into())
            }
        }.build()
}
