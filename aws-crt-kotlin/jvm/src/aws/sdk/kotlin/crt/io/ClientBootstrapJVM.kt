/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.AsyncShutdown
import aws.sdk.kotlin.crt.Closeable
import kotlinx.coroutines.future.await
import software.amazon.awssdk.crt.io.ClientBootstrap as ClientBootstrapJni

public actual class ClientBootstrap actual constructor(elg: EventLoopGroup, hr: HostResolver) :
    Closeable,
    AsyncShutdown {
    internal val jniBootstrap = ClientBootstrapJni(elg.jniElg, hr.jniHr)

    actual override fun close() {
        jniBootstrap.close()
    }

    actual override suspend fun waitForShutdown() {
        jniBootstrap.shutdownCompleteFuture.await()
    }
}
