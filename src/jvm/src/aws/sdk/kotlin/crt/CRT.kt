/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt

import software.amazon.awssdk.crt.Log
import java.util.concurrent.atomic.AtomicInteger as AtomicInt
import software.amazon.awssdk.crt.CRT as crtJni

public actual object CRT {
    private val initialized: AtomicInt = AtomicInt(0)
    public actual fun initRuntime(block: aws.sdk.kotlin.crt.Config.() -> Unit) {
        if (!initialized.compareAndSet(0, 1)) return

        System.setProperty("aws.crt.memory.tracing", "${CrtDebug.traceLevel}")
        // load the JNI library
        crtJni()
        val config = aws.sdk.kotlin.crt.Config().apply(block)
        val logLevel = Log.LogLevel.valueOf(config.logLovel.name)
        when (config.logDestination) {
            LogDestination.None -> return
            LogDestination.Stdout -> Log.initLoggingToStdout(logLevel)
            LogDestination.Stderr -> Log.initLoggingToStderr(logLevel)
            LogDestination.File -> {
                val logfile = config.logFile
                requireNotNull(logfile) { "log filename must be specified when LogDestination.File is specified" }
                Log.initLoggingToFile(logLevel, logfile)
            }
        }
    }

    /**
     * Returns the last error on the current thread.
     * @return Last error code recorded in this thread
     */
    public actual fun awsLastError(): Int = crtJni.awsLastError()

    /**
     * Given an integer error code from an internal operation
     * @param errorCode An error code returned from an exception or other native function call
     * @return A user-friendly description of the error
     */
    public actual fun awsErrorString(errorCode: Int): String? = crtJni.awsErrorString(errorCode)

    /**
     * Given an integer error code from an internal operation
     *
     * @param errorCode An error code returned from an exception or other native
     * function call
     * @return A string identifier for the error
     */
    public actual fun awsErrorName(errorCode: Int): String? = crtJni.awsErrorName(errorCode)

    /**
     * @return The number of bytes allocated in native resources. If aws.crt.memory.tracing is 1 or 2, this will
     * be a non-zero value. Otherwise, no tracing will be done, and the value will always be 0
     */
    public actual fun nativeMemory(): Long = crtJni.nativeMemory()
}
