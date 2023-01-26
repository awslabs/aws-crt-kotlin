/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.http

/**
 * Type of header block
 */
public enum class HttpHeaderBlock(public val blockType: Int) {
    MAIN(0),

    INFORMATIONAL(1),

    TRAILING(2),
}
