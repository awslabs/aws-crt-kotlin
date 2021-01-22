/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt.auth.credentials

/**
 * Represents a set of AWS credentials
 */
public data class Credentials(val accessKeyId: String, val secretAccessKey: String, val sessionToken: String?) {

    public constructor(accessKeyId: ByteArray, secretAccessKey: ByteArray, sessionToken: ByteArray?) :
        this(accessKeyId.decodeToString(), secretAccessKey.decodeToString(), sessionToken?.decodeToString())
}
