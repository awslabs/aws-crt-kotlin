/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import software.amazon.awssdk.kotlin.crt.CrtTest
import software.amazon.awssdk.kotlin.crt.runSuspendTest
import kotlin.test.Test

class ClientBootstrapTest : CrtTest() {
    @Test
    fun createDestroy() = runSuspendTest {
        val elg = EventLoopGroup()
        val hr = HostResolver(elg)
        val bootstrap = ClientBootstrap(elg, hr)
        bootstrap.close()
        hr.close()
        elg.close()
    }
}
