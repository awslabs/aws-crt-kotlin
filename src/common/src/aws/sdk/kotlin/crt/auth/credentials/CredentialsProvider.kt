/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.AsyncShutdown
import aws.sdk.kotlin.crt.Closeable

/**
 * Represents a producer/source of AWS credentials
 */
public interface CredentialsProvider : Closeable, AsyncShutdown {

    /**
     * Request credentials from the provider
     */
    public suspend fun getCredentials(): Credentials
}

// TODO - expose other crt providers available (some are reachable through default chain): sts, imds/ec2, ecs, environment, profile, process. Not all are available from crt-java yet
