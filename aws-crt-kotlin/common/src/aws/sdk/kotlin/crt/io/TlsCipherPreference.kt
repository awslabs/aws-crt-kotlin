/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

/**
 * A TlsCipherPreference represents a hardcoded ordered list of TLS Ciphers to use when negotiating a TLS Connection.
 *
 * At present, the ability to configure arbitrary orderings of TLS Ciphers is not allowed, and only a curated list of
 * vetted TlsCipherPreference's are exposed.
 */
public enum class TlsCipherPreference(public val value: Int) {
    /**
     * Use whatever the System Default Preference is. This is usually the best option, as it will be automatically
     * updated as the underlying OS or platform changes, and will always be supported on all Platforms.
     */
    SYSTEM_DEFAULT(0),

    /**
     * This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.
     */
    @Deprecated("This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.")
    KMS_PQ_TLS_V1_0_2019_06(1),

    /**
     * This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.
     */
    @Deprecated("This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.")
    KMS_PQ_SIKE_TLS_V1_0_2019_11(2),

    /**
     * This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.
     */
    @Deprecated("This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.")
    KMS_PQ_TLS_V1_0_2020_02(3),

    /**
     * This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.
     */
    @Deprecated("This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.")
    KMS_PQ_SIKE_TLS_V1_0_2020_02(4),

    /**
     * This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.
     */
    @Deprecated("This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.")
    KMS_PQ_TLS_V1_0_2020_07(5),

    /**
     * This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.
     */
    @Deprecated("This cipher preference is no longer supported. Use PQ_TLSV1_2_2024_10 instead.")
    PQ_TLS_V1_0_2021_05(6),

    /**
     * This TLS cipher preference list contains post-quantum key exchange algorithms that have been standardized by
     * NIST. PQ algorithms in this preference list will be used in hybrid mode, and always combined with a classical
     * ECDHE key exchange.
     */
    PQ_TLSV1_2_2024_10(7),

    /**
     * Recommended default policy with post-quantum algorithm support. This policy may change over time.
     */
    PQ_DEFAULT(8),

    ;

    /**
     * Not all Cipher Preferences are supported on all Platforms due to differences in the underlying TLS Libraries.
     *
     * @return True if this TlsCipherPreference is currently supported on the current platform.
     */
    public val isSupported: Boolean
        get() = TlsContextOptions.isCipherPreferenceSupported(this)
}
