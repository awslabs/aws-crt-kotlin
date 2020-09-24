/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import software.amazon.awssdk.kotlin.crt.Closeable

/**
 * This class wraps the aws_tls_context from aws-c-io to provide
 * access to TLS configuration contexts in the AWS Common Runtime.
 */
public expect class TlsContext(options: TlsContextOptions? = null) : Closeable
