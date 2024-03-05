/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.Closeable
import aws.sdk.kotlin.crt.NativeHandle
import kotlinx.cinterop.CPointer
import libcrt.aws_tls_ctx

public actual class TlsContext actual constructor(
    options: TlsContextOptions?,
) : Closeable, NativeHandle<aws_tls_ctx> {
    override val ptr: CPointer<aws_tls_ctx>
        get() = TODO("Not yet implemented")
    public actual companion object {}

    override fun close() {
        TODO("Not yet implemented")
    }
}

internal actual fun isCipherSupported(cipher: TlsCipherPreference): Boolean = TODO("Not yet implemented")
internal actual fun isAlpnSupported(): Boolean = TODO("Not yet implemented")
