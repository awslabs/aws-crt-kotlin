/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt.util

import kotlinx.coroutines.channels.Channel
import kotlinx.cinterop.StableRef

/**
 * Channel used to signal async shutdown from C callback to a suspend fn
 */
internal typealias ShutdownChannel = Channel<Unit>

/**
 * Create a new shutdown notification channel
 */
internal fun shutdownChannel(): ShutdownChannel = Channel(Channel.RENDEZVOUS)

/**
 * Execute [block] using [StableRef], then dispose it.
 */
internal inline fun <T : Any, R> StableRef<T>.use(block: (StableRef<T>) -> R): R {
    try {
        return block(this)
    } finally {
        dispose()
    }
}
