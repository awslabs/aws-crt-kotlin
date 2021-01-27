/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt

public interface Closeable {
    /**
     * Close this resource
     */
    public fun close()
}

public inline fun <C : Closeable, R> C.use(block: (C) -> R): R {
    var closed = false

    return try {
        block(this)
    } catch (first: Throwable) {
        try {
            closed = true
            close()
        } catch (second: Throwable) {
            // suppressed
        }

        throw first
    } finally {
        if (!closed) {
            close()
        }
    }
}
