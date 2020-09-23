/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

/**
 * This class wraps the aws_client_bootstrap from aws-c-io to provide
 * a client context for all protocol stacks in the AWS Common Runtime.
 */
expect class ClientBootstrap(elg: EventLoopGroup, hr: HostResolver) {

    suspend fun close()
}
