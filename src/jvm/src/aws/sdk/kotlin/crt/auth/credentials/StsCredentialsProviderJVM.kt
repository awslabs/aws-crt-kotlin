/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.auth.credentials

import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.crt.auth.credentials.Credentials
import java.util.concurrent.CompletableFuture
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider as CredentialsProviderJni
import software.amazon.awssdk.crt.auth.credentials.StsCredentialsProvider as StsCredentialsProviderJni

// Based from the Java SDK default
// https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/sts/model/AssumeRoleRequest.html#durationSeconds
private const val DEFAULT_DURATION_SECONDS = 3600

public actual class StsCredentialsProvider
internal actual constructor(builder: StsCredentialsProviderBuilder) :
    CredentialsProvider, JniCredentialsProvider() {
    public actual companion object { }

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

    private fun adapt(credentialsProvider: CredentialsProvider?): software.amazon.awssdk.crt.auth.credentials.CredentialsProvider? {
        return if (credentialsProvider == null) {
            null
        } else {
            object : software.amazon.awssdk.crt.auth.credentials.CredentialsProvider() {
                override fun getCredentials(): CompletableFuture<Credentials> {
                    val cp = CompletableFuture<Credentials>()
                    cp.completeAsync {
                        runBlocking {
                            toCrtCredentials(credentialsProvider.getCredentials())
                        }
                    }
                    return cp
                }
            }
        }
    }

    private fun toCrtCredentials(sdkCredentials: aws.sdk.kotlin.crt.auth.credentials.Credentials): Credentials {
        return object : Credentials() {
            override fun getAccessKeyId(): ByteArray {
                return sdkCredentials.accessKeyId.encodeToByteArray()
            }

            override fun getSecretAccessKey(): ByteArray {
                return sdkCredentials.accessKeyId.encodeToByteArray()
            }

            override fun getSessionToken(): ByteArray {
                return sdkCredentials.sessionToken?.encodeToByteArray() ?: ByteArray(0)
            }
        }
    }
}
