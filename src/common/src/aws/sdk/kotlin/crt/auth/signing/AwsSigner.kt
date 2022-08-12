/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.signing

import aws.sdk.kotlin.crt.http.HttpRequest

public expect object AwsSigner {
    public suspend fun signRequest(request: HttpRequest, config: AwsSigningConfig): HttpRequest

    public suspend fun sign(request: HttpRequest, config: AwsSigningConfig): AwsSigningResult

    public suspend fun signChunk(chunkBody: ByteArray, prevSignature: ByteArray, config: AwsSigningConfig): AwsSigningResult
}
