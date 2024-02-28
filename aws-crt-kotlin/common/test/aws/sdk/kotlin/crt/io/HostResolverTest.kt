/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.CrtTest
import aws.sdk.kotlin.crt.runSuspendTest
import aws.sdk.kotlin.crt.use
import kotlin.test.Ignore
import kotlin.test.Test

class HostResolverTest : CrtTest() {
    @Ignore // FIXME Enable when Kotlin/Native implementation is complete
    @Test
    fun createDestroy() = runSuspendTest {
        EventLoopGroup().use { elg ->
            val hr = HostResolver(elg)
            hr.close()
        }
    }
}
