/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.util

import Sha1Native
import Sha256Native
import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.CrtRuntimeException
import aws.sdk.kotlin.crt.awsAssertOpSuccess
import kotlinx.cinterop.*
import libcrt.*

/**
 * Utility object for various hash functions
 */
public actual object Digest {
    /**
     * Calculate the SHA-256 hash of the input [buffer]
     */
    public actual fun sha256(buffer: ByteArray): ByteArray = Sha256Native().apply {
        update(buffer, 0, buffer.size)
    }.digest()
}
