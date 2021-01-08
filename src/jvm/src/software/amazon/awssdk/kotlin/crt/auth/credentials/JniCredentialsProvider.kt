/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.auth.credentials

import kotlinx.coroutines.future.await
import software.amazon.awssdk.kotlin.crt.AsyncShutdown
import java.io.Closeable
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider as CredentialsProviderJni

/**
 * Base class for proxying JNI credentials providers
 */
public abstract class JniCredentialsProvider : CredentialsProvider, Closeable, AsyncShutdown {
    internal abstract val jniCredentials: CredentialsProviderJni

    override suspend fun getCredentials(): Credentials {
        val jniCreds = jniCredentials.credentials.await()
        return Credentials(jniCreds.accessKeyId, jniCreds.secretAccessKey, jniCreds.sessionToken)
    }

    override fun close() {
        jniCredentials.close()
    }

    override suspend fun waitForShutdown() {
        jniCredentials.shutdownCompleteFuture.await()
    }
}
