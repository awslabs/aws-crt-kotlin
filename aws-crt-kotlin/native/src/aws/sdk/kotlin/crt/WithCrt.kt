/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt

public open class WithCrt() {
    init {
        CRT.initRuntime {  }
    }
}