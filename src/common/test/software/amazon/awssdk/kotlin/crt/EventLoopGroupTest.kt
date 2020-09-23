/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import software.amazon.awssdk.kotlin.crt.io.EventLoopGroup
import kotlin.test.Test

class EventLoopGroupTest {
    @Test
    fun createDestroy() = runSuspendTest {
        val elg = EventLoopGroup()
        elg.close()
    }

    @Test
    fun createDestroyDefault() = runSuspendTest {
        val elg = EventLoopGroup.DEFAULT
        elg.close()
    }
}
