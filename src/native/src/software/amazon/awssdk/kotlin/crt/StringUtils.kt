/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
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

/**
 * Get a byte cursor from the current Kotlin string
 */
public fun String.asAwsByteCursor(): CValue<aws_byte_cursor> {
    return aws_byte_cursor_from_c_str(this)
}

/**
 * tl;dr convenience init for the way Kotlin/Native interop generates bindings.
 *
 * The bindings generated for a struct with a nested byte cursor generate the
 * property as a Kotlin `val` making it un-assignable directly.
 */
public inline fun aws_byte_cursor.initFromString(str: String) {
    val cur = str.asAwsByteCursor()
    this.initFromCursor(cur)
}

public inline fun aws_byte_cursor.initFromCursor(cur: CValue<aws_byte_cursor>) {
    val dest = this
    cur.useContents {
        dest.len = len
        dest.ptr = ptr
    }
}
