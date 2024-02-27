/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt

import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import libcrt.*

@OptIn(ExperimentalForeignApi::class)
internal object Logging {
    private var initialized = false
    private val initializerMu = Mutex() // protects `initialized`

    internal fun initialize(config: Config): Unit = runBlocking { initializerMu.withLock {
        if (initialized) { return@runBlocking }

        memScoped {
            val options = cValue<aws_logger_standard_options> {
                when (config.logDestination) {
                    LogDestination.None -> return@runBlocking
                    LogDestination.Stdout -> file = platform.posix.stdout
                    LogDestination.Stderr -> file = platform.posix.stderr
                    LogDestination.File -> filename = requireNotNull(config.logFile?.cstr?.ptr) { "LogDestination.File configured without logFile" }
                }
                level = config.logLevel.value.convert()
            }

            if (CrtDebug.traceLevel > 0) {
                awsAssertOpSuccess(aws_logger_init_noalloc(s_crt_kotlin_logger.ptr, Allocator.Default.allocator, options)) {
                    "failed to initialize no-alloc logger"
                }
            } else {
                awsAssertOpSuccess(aws_logger_init_standard(s_crt_kotlin_logger.ptr, Allocator.Default.allocator, options)) {
                    "failed to initialize standard logger"
                }
            }
        }

        aws_logger_set(s_crt_kotlin_logger.ptr)

        initialized = true
    }}
}
