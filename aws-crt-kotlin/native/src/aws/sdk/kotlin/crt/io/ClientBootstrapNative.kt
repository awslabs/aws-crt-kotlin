/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.AsyncShutdown
import aws.sdk.kotlin.crt.Closeable

public actual class ClientBootstrap actual constructor(elg: EventLoopGroup, hr: HostResolver) :
    Closeable,
    AsyncShutdown {
    actual override suspend fun waitForShutdown() {
        TODO("Not yet implemented")
    }

    actual override fun close() {
        TODO("Not yet implemented")
    }
}
