/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.util

import Sha256

/**
 * Utility object for various hash functions
 */
public actual object Digest {
    /**
     * Calculate the SHA-256 hash of the input [buffer]
     */
    public actual fun sha256(buffer: ByteArray): ByteArray = Sha256().apply {
        update(buffer, 0, buffer.size)
    }.digest()
}
