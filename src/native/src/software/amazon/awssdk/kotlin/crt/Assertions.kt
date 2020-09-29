/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import libcrt.AWS_OP_SUCCESS

/**
 * Assert the return value [ret] is AWS_OP_SUCCESS, otherwise throw CrtRuntimeException with the given
 * message provided by [lazyMessage]
 *
 * ```
 * awsAssertOp(aws_xyz_op(...)) { "xyz operation failed" }
 * ```
 */
internal inline fun awsAssertOp(ret: Int, lazyMessage: () -> String) {
    if (ret != AWS_OP_SUCCESS) {
        val message = lazyMessage()
        throw CrtRuntimeException(message)
    }
}

/**
 * Assert the return value [ret] is a non-null pointer, otherwise throw CrtRuntimeException with the given
 * message provided by [lazyMessage]
 *
 * ```
 * val ptr = awsAssertNew(aws_xyz_new(...)) { "create xyz failed" }
 * ```
 */
internal inline fun <reified T> awsAssertNotNull(ret: T?, lazyMessage: () -> String): T {
    if (ret == null) {
        val message = lazyMessage()
        throw CrtRuntimeException(message)
    }
    return ret
}
