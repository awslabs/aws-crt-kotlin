/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt.http

import aws.sdk.kotlin.runtime.crt.CRT
import aws.sdk.kotlin.runtime.crt.CrtRuntimeException

public class HttpException(override val errorCode: Int) : CrtRuntimeException(CRT.awsErrorString(errorCode))
