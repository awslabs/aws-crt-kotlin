/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

public open class CrtRuntimeException(message: String?) : RuntimeException(message) {
    public open val errorCode: Int = CRT.awsLastError()

    public val errorName: String?
        get() = CRT.awsErrorName(errorCode)

    public val errorDescription: String?
        get() = CRT.awsErrorString(errorCode)
}
