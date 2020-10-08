/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import software.amazon.awssdk.kotlin.crt.Closeable

public actual class TlsContext actual constructor(options: TlsContextOptions?) : Closeable {

    public actual companion object {
    }

    override suspend fun close() {
        TODO("Not yet implemented")
    }
}

internal actual fun isCipherSupported(cipher: TlsCipherPreference): Boolean = TODO("not yet implemented")
internal actual fun isAlpnSupported(): Boolean = TODO("not yet implemented")
