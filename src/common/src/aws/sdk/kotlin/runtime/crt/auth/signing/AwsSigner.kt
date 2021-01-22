/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt.auth.signing

import aws.sdk.kotlin.runtime.crt.http.HttpRequest

public expect object AwsSigner {
    public suspend fun signRequest(request: HttpRequest, config: AwsSigningConfig): HttpRequest
}
