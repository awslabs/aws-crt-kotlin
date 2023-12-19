/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

actual fun <T> runSuspendTest(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T {
    TODO("Not yet implemented")
}
