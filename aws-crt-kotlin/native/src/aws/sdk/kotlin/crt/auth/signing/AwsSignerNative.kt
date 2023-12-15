/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.signing

import aws.sdk.kotlin.crt.http.*

/**
 * Static class for a variety of AWS signing APIs.
 */
public actual object AwsSigner {
    public actual suspend fun signRequest(
        request: HttpRequest,
        config: AwsSigningConfig,
    ): HttpRequest {
        TODO("Not yet implemented")
    }

    public actual suspend fun sign(
        request: HttpRequest,
        config: AwsSigningConfig,
    ): AwsSigningResult {
        TODO("Not yet implemented")
    }

    public actual suspend fun signChunk(
        chunkBody: ByteArray,
        prevSignature: ByteArray,
        config: AwsSigningConfig,
    ): AwsSigningResult {
        TODO("Not yet implemented")
    }

    public actual suspend fun signChunkTrailer(
        trailingHeaders: Headers,
        prevSignature: ByteArray,
        config: AwsSigningConfig,
    ): AwsSigningResult {
        TODO("Not yet implemented")
    }
}
