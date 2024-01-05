/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt.http

public data class HttpStreamMetrics(
    val sendStartTimestampNs: Long,
    val sendEndTimestampNs: Long,
    val sendingDurationNs: Long,
    val receiveStartTimestampNs: Long,
    val receiveEndTimestampNs: Long,
    val receivingDurationNs: Long,
    val streamId: Int,
)
