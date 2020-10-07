/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.*
import libcrt.*
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.freeze

// crt.def includes a static definition "s_crt_kotlin_logger" to target since getting static pointers
// in Kotlin/Native has proven...burdensome (even hit an ICE in one of the attempts)
internal object Logging {
    private val initialized: AtomicInt = AtomicInt(0).freeze()

    internal fun initialize() {
        if (!initialized.compareAndSet(0, 1)) return

        val options = cValue<aws_logger_standard_options> {
            // fixme - propagate these options through
            file = platform.posix.stdout
            level = AWS_LOG_LEVEL_TRACE.convert()
        }

        if (CrtDebug.traceLevel > 0) {
            awsAssertOp(
                aws_logger_init_noalloc(s_crt_kotlin_logger.ptr, Allocator.Default.allocator, options)
            ) { "failed to initialize no-alloc logger" }
        } else {
            awsAssertOp(
                aws_logger_init_standard(s_crt_kotlin_logger.ptr, Allocator.Default.allocator, options)
            ) { "failed to initialize standard logger" }
        }

        aws_logger_set(s_crt_kotlin_logger.ptr)
    }
}
