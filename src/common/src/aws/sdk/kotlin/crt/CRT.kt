/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt

public expect object CRT {
    /**
     * Initialize the CRT libraries if needed
     */
    public fun initRuntime(block: Config.() -> Unit = {})

    /**
     * Returns the last error on the current thread.
     * @return Last error code recorded in this thread
     */
    public fun awsLastError(): Int

    /**
     * Given an integer error code from an internal operation
     * @param errorCode An error code returned from an exception or other native function call
     * @return A user-friendly description of the error
     */
    public fun awsErrorString(errorCode: Int): String?

    /**
     * Given an integer error code from an internal operation
     *
     * @param errorCode An error code returned from an exception or other native
     * function call
     * @return A string identifier for the error
     */
    public fun awsErrorName(errorCode: Int): String?

    /**
     * @return The number of bytes allocated in native resources. If aws.crt.memory.tracing is 1 or 2, this will
     * be a non-zero value. Otherwise, no tracing will be done, and the value will always be 0
     */
    public fun nativeMemory(): Long
}

/**
 * Configuration settings related to the common runtime
 */
public class Config {
    /**
     * The level to log internals at
     */
    public var logLovel: LogLevel = LogLevel.None

    /**
     * The destination to log to
     */
    public var logDestination: LogDestination = LogDestination.Stdout

    /**
     * The filename to write logs to. Required if [LogDestination.File] is specified
     */
    public var logFile: String? = null
}
