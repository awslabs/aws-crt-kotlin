/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import software.amazon.awssdk.kotlin.crt.CrtTest
import software.amazon.awssdk.kotlin.crt.runSuspendTest
import software.amazon.awssdk.kotlin.crt.use
import kotlin.test.Test

class HostResolverTest : CrtTest() {
    @Test
    fun createDestroy() = runSuspendTest {
        EventLoopGroup().use { elg ->
            val hr = HostResolver(elg)
            hr.close()
        }
    }
}
