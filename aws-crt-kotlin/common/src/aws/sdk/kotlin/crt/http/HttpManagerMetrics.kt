/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt.http

public data class HttpManagerMetrics(
    public val availableConcurrency: Long,
    public val pendingConcurrencyAcquires: Long,
    public val leasedConcurrency: Long,
)
