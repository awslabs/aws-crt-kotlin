/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.util

import aws.sdk.kotlin.crt.Allocator
import kotlinx.cinterop.*
import libcrt.*

/**
 * Decode an aws_string as to a kotlin [String] assuming UTF-8 bytes
 * This does NOT free the aws_string instance
 */
@OptIn(ExperimentalForeignApi::class)
public fun CPointer<aws_string>.toKString(): String? {
    val bytes = aws_string_c_str(this)
    return bytes?.toKString()
}

/**
 * Get a byte cursor from the current aws_string instance
 */
@OptIn(ExperimentalForeignApi::class)
public fun CPointer<aws_string>.asAwsByteCursor(): CValue<aws_byte_cursor> {
    return aws_byte_cursor_from_string(this)
}

/**
 * Free the aws_string instance
 */
@OptIn(ExperimentalForeignApi::class)
public fun CPointer<aws_string>.free() {
    aws_string_destroy(this)
}

/**
 * Interpret a byte cursor as a Kotlin string
 */
@OptIn(ExperimentalForeignApi::class)
public inline fun aws_byte_cursor.toKString(): String {
    return ptr?.readBytes(len.convert())?.decodeToString() ?: ""
}


// NOTE - we can't use aws_byte_cursor_from_c_str() (which takes a Kotlin string). The way Kotlin
// manages memory through this bridge is incompatible. I'm fairly certain it's because they encode the String to
// a null-terminated ByteArray, pin it, and pass the address of the starting element. This is a temporary that
// is no longer valid after the call though.
/**
 * Initialize an aws_byte_cursor from a (pinned) Kotlin [ByteArray].
 * NOTE: the cursor is only valid while the array is pinned
 */
@OptIn(ExperimentalForeignApi::class)
public fun Pinned<ByteArray>.asAwsByteCursor(): CValue<aws_byte_cursor> {
    val arr = get()
    val addr = addressOf(0)
    return cValue<aws_byte_cursor> {
        len = arr.size.convert()
        ptr = addr.reinterpret()
    }
}

/**
 * Decode the kotlin [String] as an aws_string instance
 * Caller is responsible for freeing
 */
@OptIn(ExperimentalForeignApi::class)
public fun String.toAwsString(): CPointer<aws_string> = checkNotNull(aws_string_new_from_c_str(Allocator.Default, this)) {
    "aws_string_new_from_c_string() failed using: $this"
}
