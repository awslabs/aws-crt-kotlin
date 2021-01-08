/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.auth.credentials

import java.lang.IllegalArgumentException
import software.amazon.awssdk.crt.auth.credentials.CachedCredentialsProvider as CachedCredentialsProviderJni
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider as CredentialsProviderJni

public actual class CachedCredentialsProvider internal actual constructor(builder: CachedCredentialsProviderBuilder) :
    CredentialsProvider, JniCredentialsProvider() {
    public actual companion object {}

    override val jniCredentials: CredentialsProviderJni =
        CachedCredentialsProviderJni.CachedCredentialsProviderBuilder().apply {
            // must be an instance of JNI credentials provider...
            val source = builder.source as? JniCredentialsProvider ?: throw IllegalArgumentException("unknown CachedCredentialsProvider source -- must be a CRT Java instance")

            withCachedProvider(source.jniCredentials)
            withCachingDurationInSeconds(builder.refreshTimeInMilliseconds.toInt())
        }.build()
}
