/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.io

import kotlinx.cinterop.*
import libcrt.*
import software.amazon.awssdk.kotlin.crt.*
import software.amazon.awssdk.kotlin.crt.Allocator
import software.amazon.awssdk.kotlin.crt.util.asAwsByteCursor
import software.amazon.awssdk.kotlin.crt.util.free
import software.amazon.awssdk.kotlin.crt.util.toAwsString

public actual class TlsContext actual constructor(options: TlsContextOptions?) : CrtResource<aws_tls_ctx>(), Closeable {
    private val ctx: CPointer<aws_tls_ctx>
    private val tlsCtxOpts: aws_tls_ctx_options = Allocator.Default.alloc()

    init {
        aws_tls_ctx_options_init_default_client(tlsCtxOpts.ptr, Allocator.Default)
        val kopts = options ?: TlsContextOptions.defaultClient()

        try {
            // Certs or paths will cause an init, which overwrites other fields, so do those first
            if (kopts.certificate != null && kopts.privateKey != null) {
                initClientMtls(kopts.certificate!!, kopts.privateKey!!)
            } else if (kopts.certificatePath != null && kopts.privateKeyPath != null) {
                initClientMtlsPath(kopts.certificatePath!!, kopts.privateKeyPath!!)
            }

            if (kopts.caRoot != null) {
                overrideTrustAuthority(kopts.caRoot!!)
            } else if (kopts.caFile != null || kopts.caDir != null) {
                overrideTrustAuthorityFromPath(kopts.caFile, kopts.caDir)
            }

            // FIXME - deal with apple pkcs only stuffs

            tlsCtxOpts.minimum_tls_version = kopts.minTlsVersion.value.convert()
            tlsCtxOpts.cipher_pref = kopts.tlsCipherPreference.value.convert()
            tlsCtxOpts.verify_peer = kopts.verifyPeer

            if (kopts.alpn.isNotBlank()) {
                if (aws_tls_ctx_options_set_alpn_list(tlsCtxOpts.ptr, kopts.alpn) != AWS_OP_SUCCESS) {
                    throw CrtRuntimeException("aws_tls_ctx_options_set_alpn_list()")
                }
            }
        } catch (ex: CrtRuntimeException) {
            Allocator.Default.free(tlsCtxOpts)
            throw ex
        }

        ctx = aws_tls_client_ctx_new(Allocator.Default, tlsCtxOpts.ptr) ?: throw CrtRuntimeException("aws_tls_client_ctx_new()")
    }

    // aws_tls_ctx_options_init_client_mtls()
    private fun initClientMtls(certificate: String, privateKey: String) {
        val cert = certificate.toAwsString()
        val pkey = privateKey.toAwsString()

        try {
            val certCursor = cert.asAwsByteCursor()
            val pkeyCursor = pkey.asAwsByteCursor()

            val ret = aws_tls_ctx_options_init_client_mtls(
                tlsCtxOpts.ptr,
                Allocator.Default,
                certCursor,
                pkeyCursor
            )

            if (ret != AWS_OP_SUCCESS) {
                throw CrtRuntimeException("aws_tls_ctx_options_init_client_mtls()")
            }
        } finally {
            cert.free()
            pkey.free()
        }
    }

    // aws_tls_ctx_options_init_client_mtls_from_path()
    private fun initClientMtlsPath(certPath: String, pkeyPath: String) {
        if (aws_tls_ctx_options_init_client_mtls_from_path(
                tlsCtxOpts.ptr,
                Allocator.Default,
                certPath,
                pkeyPath
            ) != AWS_OP_SUCCESS
        ) {
            throw CrtRuntimeException("aws_tls_ctx_options_init_client_mtls_from_path(): certPath: `$certPath`; keyPath: $pkeyPath")
        }
    }

    // aws_tls_ctx_options_override_default_trust_store()
    private fun overrideTrustAuthority(caRoot: String) {
        val ca = caRoot.toAwsString()
        try {
            val caCursor = ca.asAwsByteCursor()
            if (aws_tls_ctx_options_override_default_trust_store(tlsCtxOpts.ptr, caCursor) != AWS_OP_SUCCESS) {
                throw CrtRuntimeException("aws_tls_ctx_options_override_default_trust_store()")
            }
        } finally {
            ca.free()
        }
    }

    // aws_tls_ctx_options_override_default_trust_store_from_path()
    private fun overrideTrustAuthorityFromPath(caFile: String?, caPath: String?) {
        if (aws_tls_ctx_options_override_default_trust_store_from_path(
                tlsCtxOpts.ptr,
                caFile,
                caPath
            ) != AWS_OP_SUCCESS
        ) {
            throw CrtRuntimeException("aws_tls_ctx_options_override_default_trust_store_from_path()")
        }
    }

    override val ptr: CPointer<aws_tls_ctx> = ctx

    override suspend fun close() {
        aws_tls_ctx_release(ctx)
        aws_tls_ctx_options_clean_up(tlsCtxOpts.ptr)
        Allocator.Default.free(tlsCtxOpts)
    }
}

internal actual fun isCipherSupported(cipher: TlsCipherPreference): Boolean {
    return aws_tls_is_cipher_pref_supported(cipher.value.convert())
}

internal actual fun isAlpnSupported(): Boolean {
    return aws_tls_is_alpn_available()
}
