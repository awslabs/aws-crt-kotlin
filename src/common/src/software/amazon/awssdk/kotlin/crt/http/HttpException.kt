/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.http

import software.amazon.awssdk.kotlin.crt.CRT
import software.amazon.awssdk.kotlin.crt.CrtRuntimeException

public class HttpException(override val errorCode: Int) : CrtRuntimeException(CRT.awsErrorString(errorCode))
