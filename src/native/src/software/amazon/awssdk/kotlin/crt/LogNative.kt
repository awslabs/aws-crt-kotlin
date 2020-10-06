/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

import kotlinx.cinterop.*
import libcrt.*
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.freeze

// crt.def includes a static definition "s_crt_kotlin_logger" to target since getting static pointers
// in Kotlin/Native has proven...burdensome (even hit an ICE in one of the attempts)
public actual object Log {
    private val initialized: AtomicInt = AtomicInt(0).freeze()

    internal fun initialize() {
        if (!initialized.compareAndSet(0, 1)) return

        val options = cValue<aws_logger_standard_options> {
            // fixme - propagate these options through
            file = platform.posix.stderr
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

    internal fun cleanup() {
        if (aws_logger_get() == s_crt_kotlin_logger.ptr) {
            aws_logger_set(null)
        }

        if (initialized.value > 0) {
            aws_logger_clean_up(s_crt_kotlin_logger.ptr)
        }
    }

    public actual fun log(
        level: LogLevel,
        subject: LogSubject,
        message: String
    ) {
        s_crt_kotlin_log(level.value.convert(), subject.value.convert(), message)
    }

    public actual fun error(subject: LogSubject, message: String): Unit =
        log(LogLevel.Error, subject, message)

    public actual fun warn(subject: LogSubject, message: String): Unit =
        log(LogLevel.Warn, subject, message)

    public actual fun info(subject: LogSubject, message: String): Unit =
        log(LogLevel.Info, subject, message)

    public actual fun debug(subject: LogSubject, message: String): Unit =
        log(LogLevel.Debug, subject, message)

    public actual fun trace(subject: LogSubject, message: String): Unit =
        log(LogLevel.Trace, subject, message)
}
