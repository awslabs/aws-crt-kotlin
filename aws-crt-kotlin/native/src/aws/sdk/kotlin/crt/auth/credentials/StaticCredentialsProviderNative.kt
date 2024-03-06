/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

/**
 * A credentials provider for a fixed set of credentials
 */
public actual class StaticCredentialsProvider internal actual constructor(private val builder: StaticCredentialsProviderBuilder) : CredentialsProvider {
    public actual companion object {}
    override suspend fun getCredentials(): Credentials =
        Credentials(builder.accessKeyId!!, builder.secretAccessKey!!, builder.sessionToken)
    override fun close() { }
    override suspend fun waitForShutdown() { }
}
