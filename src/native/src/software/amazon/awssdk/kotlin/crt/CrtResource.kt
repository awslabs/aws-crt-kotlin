/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

import kotlinx.cinterop.AutofreeScope
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef

/**
 * This wraps a native pointer to an AWS Common Runtime resource. It also ensures
 * that the first time a resource is referenced, the CRT will be loaded and bound.
 */
public abstract class CrtResource<T : CPointed> : CValuesRef<T>() {
    // TODO - ref counting api's

    public abstract val ptr: CPointer<T>

    override fun getPointer(scope: AutofreeScope): CPointer<T> = ptr
}
