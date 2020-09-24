/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.toKString
import libcrt.*

/**
 * Decode an aws_string as to a kotlin [String] assuming UTF-8 bytes
 * This does NOT free the aws_string instance
 */
public fun CPointer<aws_string>.toKString(): String? {
    val bytes = aws_string_c_str(this)
    return bytes?.toKString()
}

/**
 * Get a byte cursor from the current aws_string instance
 */
public fun CPointer<aws_string>.asAwsByteCursor(): CValue<aws_byte_cursor> {
    return aws_byte_cursor_from_string(this)
}

/**
 * Get a byte cursor from the current aws_string instance
 */
public fun CPointer<aws_string>.free() {
    aws_string_destroy(this)
}

/**
 * Decode the kotlin [String] as an aws_string instance
 * Caller is responsible for freeing
 */
public fun String.toAwsString(): CPointer<aws_string> {
    return aws_string_new_from_c_str(Allocator.Default, this) ?: throw CrtRuntimeException("aws_string_new_from_c_string($this)")
}
