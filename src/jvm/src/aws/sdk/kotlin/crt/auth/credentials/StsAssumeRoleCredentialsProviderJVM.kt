/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.auth.credentials

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import software.amazon.awssdk.crt.auth.credentials.Credentials as CredentialsJni
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider as CredentialsProviderJni
import software.amazon.awssdk.crt.auth.credentials.StsCredentialsProvider as StsCredentialsProviderJni

// Based from the Java SDK default
// https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/sts/model/AssumeRoleRequest.html#durationSeconds
private const val DEFAULT_DURATION_SECONDS = 3600

public actual class StsAssumeRoleCredentialsProvider
internal actual constructor(builder: StsAssumeRoleCredentialsProviderBuilder) :
    CredentialsProvider, JniCredentialsProvider() {
    public actual companion object {}

    override val jniCredentials: CredentialsProviderJni =
        StsCredentialsProviderJni
            .builder()
            .withClientBootstrap(builder.clientBootstrap?.jniBootstrap)
            .withTlsContext(builder.tlsContext?.jniCtx)
            .withCredsProvider(adapt(builder.credentialsProvider))
            .withDurationSeconds(builder.durationSeconds ?: DEFAULT_DURATION_SECONDS)
            .withRoleArn(builder.roleArn)
            .withSessionName(builder.sessionName)
            .build()
}

// Convert the SDK version of CredentialsProvider type to the CRT version
internal fun adapt(credentialsProvider: CredentialsProvider?): CredentialsProviderJni? {
    return if (credentialsProvider == null) {
        null
    } else {
        toCrtCredentialsProvider(credentialsProvider)
    }
}

private fun toCrtCredentialsProvider(sdkCredentialsProvider: CredentialsProvider): CredentialsProviderJni {
    return object : CredentialsProviderJni() {

        override fun getCredentials(): CompletableFuture<CredentialsJni> = runBlocking {
            val deferred = async(start = CoroutineStart.UNDISPATCHED) {
                toCrtCredentials(sdkCredentialsProvider.getCredentials())
            }

            deferred.asCompletableFuture()
        }
    }
}

private fun toCrtCredentials(sdkCredentials: Credentials): CredentialsJni {
    return object : CredentialsJni() {
        override fun getAccessKeyId(): ByteArray {
            return sdkCredentials.accessKeyId.encodeToByteArray()
        }

        override fun getSecretAccessKey(): ByteArray {
            return sdkCredentials.secretAccessKey.encodeToByteArray()
        }

        override fun getSessionToken(): ByteArray {
            return sdkCredentials.sessionToken?.encodeToByteArray() ?: ByteArray(0)
        }
    }
}
