/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

public actual class ClientBootstrap actual constructor(elg: EventLoopGroup, hr: HostResolver) {
    // TODO - proxy to JNI

    public actual suspend fun close() {
        TODO("not implemented")
    }
}
