/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

public actual class X509CredentialsProvider internal actual constructor(builder: X509CredentialsProviderBuilder) : CredentialsProvider {

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
