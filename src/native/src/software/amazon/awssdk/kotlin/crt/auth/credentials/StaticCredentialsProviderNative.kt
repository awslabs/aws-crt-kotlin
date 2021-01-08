/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.auth.credentials

import software.amazon.awssdk.kotlin.crt.AsyncShutdown
import software.amazon.awssdk.kotlin.crt.Closeable

/**
 * A credentials provider for a fixed set of credentials
 */
public actual class StaticCredentialsProvider internal actual constructor(builder: StaticCredentialsProviderBuilder) :
    CredentialsProvider, AsyncShutdown, Closeable {

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
