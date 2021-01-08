/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.util

import java.security.MessageDigest

public actual object Digest {

    /**
     * Calculate the SHA-256 hash of the input [buffer]
     */
    public actual fun sha256(buffer: kotlin.ByteArray): kotlin.ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(buffer)
        return digest.digest()
    }
}
