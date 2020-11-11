/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.util

public actual object Digest {
    /**
     * Calculate the SHA-256 hash of the input [buffer]
     */
    public actual fun sha256(buffer: ByteArray): ByteArray {
        TODO("Not yet implemented - add calls to aws-cal sha256 functions")
    }
}
