/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.auth.signing

import software.amazon.awssdk.kotlin.crt.http.HttpRequest

public actual object AwsSigner {
    public actual suspend fun signRequest(request: HttpRequest, config: AwsSigningConfig): HttpRequest = TODO()
}
