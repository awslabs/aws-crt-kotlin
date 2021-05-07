/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.http

import aws.sdk.kotlin.crt.CRT
import aws.sdk.kotlin.crt.CrtRuntimeException

public class HttpException(override val errorCode: Int) : CrtRuntimeException(CRT.errorString(errorCode))
