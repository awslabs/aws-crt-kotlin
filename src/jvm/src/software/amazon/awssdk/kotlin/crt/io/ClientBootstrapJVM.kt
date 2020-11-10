/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import kotlinx.coroutines.future.await
import software.amazon.awssdk.kotlin.crt.Closeable
import software.amazon.awssdk.crt.io.ClientBootstrap as ClientBootstrapJni

public actual class ClientBootstrap actual constructor(elg: EventLoopGroup, hr: HostResolver) : Closeable {
    internal val jniBootstrap = ClientBootstrapJni(elg.jniElg, hr.jniHr)

    override suspend fun close() {
        jniBootstrap.close()
        jniBootstrap.shutdownCompleteFuture.await()
    }
}
