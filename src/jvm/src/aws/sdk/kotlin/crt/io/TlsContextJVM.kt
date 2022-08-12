/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.io

import aws.sdk.kotlin.crt.Closeable
import software.amazon.awssdk.crt.io.TlsCipherPreference as TlsCipherPreferenceJni
import software.amazon.awssdk.crt.io.TlsContext as TlsContextJni
import software.amazon.awssdk.crt.io.TlsContextOptions as TlsContextOptionsJni
import software.amazon.awssdk.crt.io.TlsContextOptions.TlsVersions as TlsVersionJni

public actual class TlsContext actual constructor(options: TlsContextOptions?) : Closeable {
    internal val jniCtx = if (options != null) TlsContextJni(options.into()) else TlsContextJni()

    public actual companion object {
    }

    override fun close() {
        jniCtx.close()
    }
}

internal actual fun isCipherSupported(cipher: TlsCipherPreference): Boolean =
    TlsContextOptionsJni.isCipherPreferenceSupported(cipher.into())

internal actual fun isAlpnSupported(): Boolean = TlsContextOptionsJni.isAlpnSupported()

private fun TlsCipherPreference.into(): TlsCipherPreferenceJni = when (this) {
    TlsCipherPreference.SYSTEM_DEFAULT -> TlsCipherPreferenceJni.TLS_CIPHER_SYSTEM_DEFAULT
    TlsCipherPreference.KMS_PQ_TLSv1_0_2019_06 -> TlsCipherPreferenceJni.TLS_CIPHER_KMS_PQ_TLSv1_0_2019_06
    TlsCipherPreference.KMS_PQ_SIKE_TLSv1_0_2019_11 -> TlsCipherPreferenceJni.TLS_CIPHER_PREF_KMS_PQ_SIKE_TLSv1_0_2019_11
    TlsCipherPreference.KMS_PQ_TLSv1_0_2020_02 -> TlsCipherPreferenceJni.TLS_CIPHER_PREF_KMS_PQ_TLSv1_0_2020_02
    TlsCipherPreference.KMS_PQ_SIKE_TLSv1_0_2020_02 -> TlsCipherPreferenceJni.TLS_CIPHER_PREF_KMS_PQ_SIKE_TLSv1_0_2020_02
    TlsCipherPreference.KMS_PQ_TLSv1_0_2020_07 -> TlsCipherPreferenceJni.TLS_CIPHER_PREF_KMS_PQ_TLSv1_0_2020_07
}

private fun TlsVersion.into(): TlsVersionJni = when (this) {
    TlsVersion.SSLv3 -> TlsVersionJni.SSLv3
    TlsVersion.TLSv1 -> TlsVersionJni.TLSv1
    TlsVersion.TLSv1_1 -> TlsVersionJni.TLSv1_1
    TlsVersion.TLSv1_2 -> TlsVersionJni.TLSv1_2
    TlsVersion.TLSv1_3 -> TlsVersionJni.TLSv1_3
    TlsVersion.SYS_DEFAULT -> TlsVersionJni.TLS_VER_SYS_DEFAULTS
}

private fun TlsContextOptions.into(): TlsContextOptionsJni {
    val kopts = this
    val jniOpts: TlsContextOptionsJni

    // Certs or paths will cause an init, which overwrites other fields, so do those first
    if (kopts.certificate != null && kopts.privateKey != null) {
        jniOpts = TlsContextOptionsJni.createWithMtls(kopts.certificate, kopts.privateKey)
    } else if (kopts.certificatePath != null && kopts.privateKeyPath != null) {
        jniOpts = TlsContextOptionsJni.createWithMtlsFromPath(kopts.certificatePath, kopts.privateKeyPath)
    } else {
        jniOpts = TlsContextOptionsJni.createDefaultClient()
    }

    if (kopts.caRoot != null) {
        jniOpts.withCertificateAuthority(kopts.caRoot)
    } else if (kopts.caFile != null || kopts.caDir != null) {
        jniOpts.withCertificateAuthorityFromPath(kopts.caFile, kopts.caDir)
    }

    jniOpts.withMinimumTlsVersion(kopts.minTlsVersion.into())
        .withCipherPreference(kopts.tlsCipherPreference.into())
        .withVerifyPeer(kopts.verifyPeer)

    if (kopts.alpn.isNotEmpty()) {
        jniOpts.withAlpnList(kopts.alpn)
    }

    return jniOpts
}
