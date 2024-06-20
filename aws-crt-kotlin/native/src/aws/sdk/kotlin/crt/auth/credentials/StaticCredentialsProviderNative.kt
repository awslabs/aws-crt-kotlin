/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

/**
 * A credentials provider for a fixed set of credentials
 */
public actual class StaticCredentialsProvider internal actual constructor(builder: StaticCredentialsProviderBuilder) :
    CredentialsProvider {
    public actual companion object {}

    actual override suspend fun getCredentials(): Credentials {
        TODO("Not yet implemented")
    }

    actual override fun close() {
        TODO("Not yet implemented")
    }

    actual override suspend fun waitForShutdown() {
        TODO("Not yet implemented")
    }
}
