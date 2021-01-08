/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// TODO - replace with kotlinx-coroutines-test if we can figure out the gradle mess
/**
 * MPP compatible runBlocking to run suspend tests in common modules
 */
expect fun <T> runSuspendTest(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T
