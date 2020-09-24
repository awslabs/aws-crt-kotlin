/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import software.amazon.awssdk.kotlin.crt.io.TlsContext
import kotlin.test.Test

class TlsContextTest : CrtTest() {
    @Test
    fun createDestroy() = runSuspendTest {
        val ctx = TlsContext()
        ctx.close()
    }
}
