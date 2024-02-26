/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt

import kotlinx.cinterop.*
import libcrt.*
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner

@OptIn(ExperimentalForeignApi::class)
public abstract class CrtResource<T: CPointed> : CValuesRef<T>() {

    public abstract val ptr: CPointer<T>

    override fun getPointer(scope: AutofreeScope): CPointer<T> = ptr

    private val rc = cValue<aws_ref_count>()

    init {
        aws_ref_count_init(
            ref_count = rc,
            `object` = ptr,
            on_zero_fn = null
        )
    }

    // FIXME is `ptr` the thing this cleaner should be referencing, or something else?
    @OptIn(ExperimentalNativeApi::class)
    internal val cleaner = createCleaner(ptr) {
        aws_ref_count_release(rc)
    }

    // FIXME do we need to provide explicit acquire/release if we use a cleaner?
    public fun acquire() { aws_ref_count_acquire(rc) }
    public fun release() { aws_ref_count_release(rc) }
}