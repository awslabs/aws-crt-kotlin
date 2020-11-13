/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.auth.credentials

public actual class CachedCredentialsProvider internal actual constructor(builder: CachedCredentialsProviderBuilder) :
    CredentialsProvider {
    public actual companion object {}

    override suspend fun getCredentials(): Credentials {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override suspend fun waitForShutdown() {
        TODO("Not yet implemented")
    }
}
