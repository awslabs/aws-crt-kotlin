/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt

public open class CrtRuntimeException(message: String?) : RuntimeException(message) {
    public open val errorCode: Int = CRT.lastError()

    public val errorName: String?
        get() = CRT.errorName(errorCode)

    public val errorDescription: String?
        get() = CRT.errorString(errorCode)
}
