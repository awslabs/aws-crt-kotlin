/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

public enum class TlsVersion(public val value: Int) {
    /**
     * SSL v3. This should almost never be used.
     */
    SSLv3(0),

    TLSv1(1),
    /**
     * TLS 1.1
     */
    TLSv1_1(2),

    /**
     * TLS 1.2
     */
    TLSv1_2(3),

    /**
     * TLS 1.3
     */
    TLSv1_3(4),

    /**
     * Use whatever the system default is. This is usually the best option, as it will be automatically updated
     * as the underlying OS or platform changes.
     */
    SYS_DEFAULT(128);
}
