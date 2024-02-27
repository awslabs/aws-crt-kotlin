/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.CrtTest
import aws.sdk.kotlin.crt.runSuspendTest
import kotlin.test.Ignore
import kotlin.test.Test

class TlsContextTest : CrtTest() {
    @Ignore // FIXME Enable when Kotlin/Native implementation is complete
    @Test
    fun createDestroy() = runSuspendTest {
        val ctx = TlsContext()
        ctx.close()
    }
}
