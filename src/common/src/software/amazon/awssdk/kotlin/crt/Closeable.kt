/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

public interface Closeable {
    /**
     * Resource shutdown may be async
     */
    public suspend fun close()
}

public suspend inline fun <C : Closeable, R> C.use(block: (C) -> R): R {
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
