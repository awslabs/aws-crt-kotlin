/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import software.amazon.awssdk.kotlin.crt.CrtTest
import software.amazon.awssdk.kotlin.crt.runSuspendTest
import kotlin.test.Test

class TlsContextTest : CrtTest() {
    @Test
    fun createDestroy() = runSuspendTest {
        val ctx = TlsContext()
        ctx.close()
    }
}
