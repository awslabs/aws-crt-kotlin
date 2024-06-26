/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials
import software.amazon.awssdk.crt.auth.credentials.StaticCredentialsProvider as StaticCredentialsProviderJni

/**
 * A credentials provider for a fixed set of credentials
 */
public actual class StaticCredentialsProvider internal actual constructor(builder: StaticCredentialsProviderBuilder) :
    JniCredentialsProvider(),
    CredentialsProvider {
    public actual companion object {}

    override val jniCredentials = StaticCredentialsProviderJni.StaticCredentialsProviderBuilder().apply {
        // build() requires these be non-null already
        withAccessKeyId(builder.accessKeyId!!.encodeToByteArray())
        withSecretAccessKey(builder.secretAccessKey!!.encodeToByteArray())
        builder.sessionToken?.let {
            withSessionToken(it.encodeToByteArray())
        }
    }.build()
}
