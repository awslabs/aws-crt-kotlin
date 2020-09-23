/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import software.amazon.awssdk.kotlin.crt.Closeable

public actual class ClientBootstrap actual constructor(elg: EventLoopGroup, hr: HostResolver) : Closeable {
    // TODO - proxy to JNI

    override suspend fun close() {
        TODO("not implemented")
    }
}
