/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt

public actual object CRT {
    /**
     * Initialize the CRT libraries if needed
     */
    public actual fun initRuntime(block: Config.() -> Unit) { }

    /**
     * Returns the last error on the current thread.
     * @return Last error code recorded in this thread
     */
    public actual fun lastError(): Int {
        TODO("Not yet implemented")
    }

    /**
     * Given an integer error code from an internal operation
     * @param errorCode An error code returned from an exception or other native function call
     * @return A user-friendly description of the error
     */
    public actual fun errorString(errorCode: Int): String? {
        TODO("Not yet implemented")
    }

    /**
     * Given an integer error code from an internal operation
     *
     * @param errorCode An error code returned from an exception or other native
     * function call
     * @return A string identifier for the error
     */
    public actual fun errorName(errorCode: Int): String? {
        TODO("Not yet implemented")
    }

    public actual fun isHttpErrorRetryable(errorCode: Int): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * @return The number of bytes allocated in native resources. If aws.crt.memory.tracing is 1 or 2, this will
     * be a non-zero value. Otherwise, no tracing will be done, and the value will always be 0
     */
    public actual fun nativeMemory(): Long {
        TODO("Not yet implemented")
    }
}
