/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt

import kotlinx.atomicfu.atomic
import kotlinx.cinterop.*
import libcrt.*

@OptIn(ExperimentalForeignApi::class)
internal object Logging {
    private val initialized = atomic(false)

    internal fun initialize(config: Config) {
        if (!initialized.compareAndSet(false, true)) return

        memScoped {
            val options = cValue<aws_logger_standard_options> {
                when (config.logDestination) {
                    LogDestination.None -> return
                    LogDestination.Stdout -> file = platform.posix.stdout
                    LogDestination.Stderr -> file = platform.posix.stderr
                    LogDestination.File -> { filename = config.logFile?.cstr?.ptr
                        ?: throw IllegalArgumentException("LogDestination.File configured without logFile")
                    }
                }
                level = config.logLevel.value.convert()
            }

            if (CrtDebug.traceLevel > 0) {
                aws_logger_init_noalloc(s_crt_kotlin_logger.ptr, Allocator.Default.allocator, options).awsAssertOpSuccess {
                    "failed to initialize no-alloc logger"
                }
            } else {
                aws_logger_init_standard(s_crt_kotlin_logger.ptr, Allocator.Default.allocator, options).awsAssertOpSuccess {
                    "failed to initialize standard logger"
                }
            }
        }

        aws_logger_set(s_crt_kotlin_logger.ptr)
    }
}