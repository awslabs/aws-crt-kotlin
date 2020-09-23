/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.CPointer
import libcrt.aws_allocator
import libcrt.aws_default_allocator

internal object Allocator {
    val Default: CPointer<aws_allocator> = aws_default_allocator() ?: throw CrtRuntimeException("default allocator init failed")
}
