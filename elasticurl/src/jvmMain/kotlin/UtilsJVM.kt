/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.runBlocking as kotlinRunBlocking

internal actual fun <T> runBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    kotlinRunBlocking { block() }

internal actual fun platformInit() { }
