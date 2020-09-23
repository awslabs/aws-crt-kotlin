/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

public class CrtRuntimeException(message: String?) : RuntimeException(message) {
    public val errorCode: Int = CRT.awsLastError()
    public val errorName: String? = CRT.awsErrorName(errorCode)
}
