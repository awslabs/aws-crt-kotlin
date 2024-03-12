/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt.util

import kotlinx.coroutines.channels.Channel

/**
 * Channel used to signal async shutdown from C callback to a suspend fn
 */
internal typealias ShutdownChannel = Channel<Unit>

/**
 * Create a new shutdown notification channel
 */
internal fun shutdownChannel(): ShutdownChannel = Channel(Channel.RENDEZVOUS)
