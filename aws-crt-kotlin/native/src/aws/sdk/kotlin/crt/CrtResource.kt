/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt

import kotlinx.cinterop.*
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
public abstract class CrtResource<T : CPointed> : CValuesRef<T>() {

    public abstract val ptr: CPointer<T>

    // FIXME - how is scope meant to be used...
    override fun getPointer(scope: AutofreeScope): CPointer<T> = ptr

    /**
     * Acquire a reference to this resource
     */
    public fun acquire() {}

    /**
     * Release a previously-acquired reference to this resource
     */
    public fun release() {}
}

// internal interface RefCounted {
//     fun acquire(): Long
//     fun release(): Long
// }

// @OptIn(ExperimentalNativeApi::class)
// internal class NativeResource<T>(
//     val value: T,
//     private val releaseResource: (T) -> Unit = {}
// ){
//     private val rc = atomic(1L)
//     public val refCount: Long
//         get() = rc.value
//     //
//     // protected open fun cleanup() {
//     // }
//
//     public fun acquire(): Long {
//         val old = rc.getAndIncrement()
//         println("acquiring ref to $this (existing=$old)")
//         check(old > 0) { "refcount has been zero, invalid to use resource again" }
//         return old
//     }
//
//     public fun release(): Long {
//         println("releasing resource: $this")
//         val old = rc.getAndDecrement()
//         check(old > 0L) { "ref count negative: $old; resource=$this" }
//         if (old == 1L) {
//             releaseResource(value)
//         }
//         println("remaining references to $this: $old")
//
//         return old - 1
//     }
// }
//
