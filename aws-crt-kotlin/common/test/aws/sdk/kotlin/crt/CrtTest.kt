/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt

import kotlinx.coroutines.runBlocking

open class CrtTest {
    init {
        runBlocking {
            CRT.initRuntime {
                logDestination = LogDestination.Stdout
                logLevel = LogLevel.Debug
            }
        }
    }
}
