/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.Closeable

public actual class TlsContext actual constructor(options: TlsContextOptions?) : Closeable {
    public actual companion object {}

    override fun close() {
        TODO("Not yet implemented")
    }
}

internal actual fun isCipherSupported(cipher: TlsCipherPreference): Boolean = TODO("Not yet implemented")
internal actual fun isAlpnSupported(): Boolean = TODO("Not yet implemented")
