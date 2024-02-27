/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt

import kotlinx.cinterop.ExperimentalForeignApi
import libcrt.AWS_OP_SUCCESS

/**
 * Assert that this [Int] return value is `AWS_OP_SUCCESS`. If not, throw [CrtRuntimeException] with the given evaluated
 * [lazyMessage].
 */
@OptIn(ExperimentalForeignApi::class)
internal inline fun Int.awsAssertOpSuccess(lazyMessage: () -> String) {
    if (this != AWS_OP_SUCCESS) {
        throw CrtRuntimeException(lazyMessage())
    }
}
