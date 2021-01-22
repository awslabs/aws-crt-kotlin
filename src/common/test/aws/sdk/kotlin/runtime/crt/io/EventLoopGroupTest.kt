/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt.io

import aws.sdk.kotlin.runtime.crt.CrtTest
import aws.sdk.kotlin.runtime.crt.runSuspendTest
import kotlin.test.Test

class EventLoopGroupTest : CrtTest() {
    @Test
    fun createDestroy() = runSuspendTest {
        val elg = EventLoopGroup()
        elg.close()
    }
}
