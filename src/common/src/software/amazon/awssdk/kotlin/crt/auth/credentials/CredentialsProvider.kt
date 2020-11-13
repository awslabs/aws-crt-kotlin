/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.auth.credentials

import software.amazon.awssdk.kotlin.crt.AsyncShutdown
import software.amazon.awssdk.kotlin.crt.Closeable

/**
 * Represents a producer/source of AWS credentials
 */
public interface CredentialsProvider : Closeable, AsyncShutdown {

    /**
     * Request credentials from the provider
     */
    public suspend fun getCredentials(): Credentials
}
