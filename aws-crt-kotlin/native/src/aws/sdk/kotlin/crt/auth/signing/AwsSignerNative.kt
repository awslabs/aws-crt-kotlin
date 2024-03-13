/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.signing

import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.auth.credentials.toNativeCredentials
import aws.sdk.kotlin.crt.awsAssertOpSuccess
import aws.sdk.kotlin.crt.http.*
import aws.sdk.kotlin.crt.util.asAwsByteCursor
import aws.sdk.kotlin.crt.util.initFromCursor
import aws.sdk.kotlin.crt.util.toAwsString
import aws.sdk.kotlin.crt.util.toKString
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import libcrt.*

/**
 * Static class for a variety of AWS signing APIs.
 */
public actual object AwsSigner {
    public actual suspend fun signRequest(
        request: HttpRequest,
        config: AwsSigningConfig,
    ): HttpRequest = checkNotNull(sign(request, config).signedRequest) { "received null signed request" }

    public actual suspend fun sign(
        request: HttpRequest,
        config: AwsSigningConfig,
    ): AwsSigningResult {
        val nativeRequest = request.toNativeRequest()

        val nativeSigningResult: CPointer<aws_signing_result> = memScoped {
            val callbackChannel = Channel<CPointer<aws_signing_result>>(Channel.RENDEZVOUS)
            val callbackChannelStableRef = StableRef.create(callbackChannel)

            val signable = aws_signable_new_http_request(
                allocator = Allocator.Default.allocator,
                request = nativeRequest
            )

            val signingCompleteCallback = staticCFunction(::signCallback)

            val nativeConfig = config
                .toNativeSigningConfig()
                .getPointer(this@memScoped)
                .reinterpret<aws_signing_config_base>()

            awsAssertOpSuccess(aws_sign_request_aws(
                allocator = Allocator.Default.allocator,
                signable = signable,
                base_config = nativeConfig,
                on_complete = signingCompleteCallback,
                userdata = callbackChannelStableRef.asCPointer(),
            )) { "sign() aws_sign_request_aws" }

            // wait for async signing to complete....
            runBlocking { callbackChannel.receive() }.also {
                callbackChannelStableRef.dispose()
                callbackChannel.close()
            }
        }

        awsAssertOpSuccess(aws_apply_signing_result_to_http_request(nativeRequest, Allocator.Default.allocator, nativeSigningResult)) {
            "sign() aws_apply_signing_result_to_http_request"
        }

        return AwsSigningResult(nativeRequest.toHttpRequest(), nativeSigningResult.getSignature())
    }

    public actual suspend fun signChunk(
        chunkBody: ByteArray,
        prevSignature: ByteArray,
        config: AwsSigningConfig,
    ): AwsSigningResult {
        val nativeSigningResult: CPointer<aws_signing_result> = memScoped {
            val callbackChannel = Channel<CPointer<aws_signing_result>>(Channel.RENDEZVOUS)
            val callbackChannelStableRef = StableRef.create(callbackChannel)

            val signable = chunkBody.usePinned {  chunkBodyPinned ->
                val chunkInputStream: CValuesRef<aws_input_stream> = checkNotNull(
                    aws_input_stream_new_from_cursor(
                        Allocator.Default.allocator,
                        chunkBodyPinned.asAwsByteCursor()
                    )) { "signChunk() aws_input_stream_new_from_cursor" }

                prevSignature.usePinned { prevSignaturePinned ->
                    aws_signable_new_chunk(Allocator.Default.allocator, chunkInputStream, prevSignaturePinned.asAwsByteCursor())
                }
            }

            val nativeConfig = config
                .toNativeSigningConfig()
                .getPointer(this@memScoped)
                .reinterpret<aws_signing_config_base>()

            awsAssertOpSuccess(aws_sign_request_aws(
                allocator = Allocator.Default.allocator,
                signable = signable,
                base_config = nativeConfig,
                on_complete = staticCFunction(::signCallback),
                userdata = callbackChannelStableRef.asCPointer(),
            )) { "signChunk() aws_sign_request_aws" }

            // wait for async signing to complete....
            runBlocking { callbackChannel.receive() }.also {
                callbackChannelStableRef.dispose()
                callbackChannel.close()
            }
        }

        return AwsSigningResult(null, nativeSigningResult.getSignature())
    }

    public actual suspend fun signChunkTrailer(
        trailingHeaders: Headers,
        prevSignature: ByteArray,
        config: AwsSigningConfig,
    ): AwsSigningResult {
        val nativeSigningResult: CPointer<aws_signing_result> = memScoped {
            val callbackChannel = Channel<CPointer<aws_signing_result>>(Channel.RENDEZVOUS)
            val callbackChannelStableRef = StableRef.create(callbackChannel)

            val nativeTrailingHeaders = aws_http_headers_new(Allocator.Default.allocator)
            trailingHeaders.forEach { key, values ->
                key.encodeToByteArray().usePinned { keyPinned ->
                    val keyCursor = keyPinned.asAwsByteCursor()
                    values.forEach { it.encodeToByteArray().usePinned { valuePinned ->
                        val valueCursor = valuePinned.asAwsByteCursor()
                        awsAssertOpSuccess(aws_http_headers_add(nativeTrailingHeaders, keyCursor, valueCursor)) {
                            "signChunkTrailer() aws_http_headers_add"
                        }
                    }}
                }
            }

            val signable = aws_signable_new_trailing_headers(
                Allocator.Default.allocator,
                nativeTrailingHeaders,
                prevSignature.usePinned { it.asAwsByteCursor() }
            )

            val nativeConfig = config
                .toNativeSigningConfig()
                .getPointer(this@memScoped)
                .reinterpret<aws_signing_config_base>()

            awsAssertOpSuccess(aws_sign_request_aws(
                allocator = Allocator.Default.allocator,
                signable = signable,
                base_config = nativeConfig,
                on_complete = staticCFunction(::signCallback),
                userdata = callbackChannelStableRef.asCPointer(),
            )) { "signChunkTrailer() aws_sign_request_aws" }

            // wait for async signing to complete....
            runBlocking { callbackChannel.receive() }.also {
                callbackChannelStableRef.dispose()
                callbackChannel.close()
            }
        }

        return AwsSigningResult(null, nativeSigningResult.getSignature())
    }
}

private fun CPointer<aws_signing_result>.getSignature(): ByteArray {
    // "The signature is found in the "signature" property on the signing result."
    // https://github.com/awslabs/aws-c-auth/blob/0d2aa00ae70c699fcb14d0338c1b07a58b9eb24b/include/aws/auth/signing.h#L26
    val signature = Allocator.Default.alloc<CPointerVar<aws_string>>()
    val propertyName = "signature".toAwsString()

    awsAssertOpSuccess(aws_signing_result_get_property(this, propertyName, signature.ptr)) {
        "aws_signing_result_get_property"
    }

    return checkNotNull(signature.value?.toKString()?.encodeToByteArray()) { "signature was null" }
}

private fun AwsSigningAlgorithm.toNativeSigningAlgorithm(): aws_signing_algorithm = when (this) {
    AwsSigningAlgorithm.SIGV4 -> aws_signing_algorithm.AWS_SIGNING_ALGORITHM_V4
    AwsSigningAlgorithm.SIGV4_ASYMMETRIC -> aws_signing_algorithm.AWS_SIGNING_ALGORITHM_V4_ASYMMETRIC
    else -> error("Signing algorithm $this is not supported.")
}

private fun AwsSignatureType.toNativeSignatureType(): aws_signature_type = when (this) {
    AwsSignatureType.HTTP_REQUEST_VIA_HEADERS -> aws_signature_type.AWS_ST_HTTP_REQUEST_HEADERS
    AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS -> aws_signature_type.AWS_ST_HTTP_REQUEST_QUERY_PARAMS
    AwsSignatureType.HTTP_REQUEST_EVENT -> aws_signature_type.AWS_ST_HTTP_REQUEST_EVENT
    AwsSignatureType.HTTP_REQUEST_CHUNK -> aws_signature_type.AWS_ST_HTTP_REQUEST_CHUNK
    AwsSignatureType.HTTP_REQUEST_TRAILING_HEADERS -> aws_signature_type.AWS_ST_HTTP_REQUEST_TRAILING_HEADERS
    else -> error("Signature type $this is not supported.")
}

private fun AwsSignedBodyHeaderType.toNativeSignedBodyHeaderType() = when (this) {
    AwsSignedBodyHeaderType.NONE -> aws_signed_body_header_type.AWS_SBHT_NONE
    AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256 -> aws_signed_body_header_type.AWS_SBHT_X_AMZ_CONTENT_SHA256
}

private fun AwsSigningConfig.toNativeSigningConfig(): CValuesRef<aws_signing_config_aws> {
    val nativeAlgorithm = algorithm.toNativeSigningAlgorithm()
    val nativeCredentials = credentials?.toNativeCredentials()

    return cValue<aws_signing_config_aws> {
        algorithm = nativeAlgorithm
        config_type = AWS_SIGNING_CONFIG_AWS

        credentials = nativeCredentials

        // FIXME implement Kotlin->CRT credentials provider conversion
//        credentials_provider =

        aws_date_time_init_epoch_millis(date.ptr, this@toNativeSigningConfig.date.toULong())
        expiration_in_seconds = this@toNativeSigningConfig.expirationInSeconds.toULong()

        flags.use_double_uri_encode = if (this@toNativeSigningConfig.useDoubleUriEncode) 1u else 0u
        flags.should_normalize_uri_path = if (this@toNativeSigningConfig.normalizeUriPath) 1u else 0u
        flags.omit_session_token = if (this@toNativeSigningConfig.omitSessionToken) 1u else 0u

        region.initFromCursor(this@toNativeSigningConfig.region.toAwsString().asAwsByteCursor())
        service.initFromCursor(this@toNativeSigningConfig.service.toAwsString().asAwsByteCursor())

        // FIXME Can't convert Kotlin config's shouldSignHeader to a C function without capturing the variable, and staticCFunction cannot capture variables.
//        should_sign_header = this@toNativeSigningConfig.shouldSignHeader?.toNativeShouldSignHeaderFn()

        signature_type = this@toNativeSigningConfig.signatureType.toNativeSignatureType()
        signed_body_header = this@toNativeSigningConfig.signedBodyHeader.toNativeSignedBodyHeaderType()
        this@toNativeSigningConfig.signedBodyValue?.let { signed_body_value.initFromCursor(it.toAwsString().asAwsByteCursor()) }
    }
}

//private fun ShouldSignHeaderFn.toNativeShouldSignHeaderFn(): CPointer<aws_should_sign_header_fn> =
//    staticCFunction { byteCursor, _  ->
//        val kString = byteCursor?.pointed?.toKString()
//        kString?.let { this(kString) } ?: false
//    }

private fun signCallback(signingResult: CPointer<aws_signing_result>?, errorCode: Int, userData: COpaquePointer?) {
    awsAssertOpSuccess(errorCode) { "signing failed" }
    checkNotNull(signingResult) { "signing callback received null aws_signing_result" }
    val channel: Channel<CPointer<aws_signing_result>> = checkNotNull(userData?.asStableRef<Channel<CPointer<aws_signing_result>>>()?.get()) { "received null userData" }
    runBlocking { channel.send(signingResult) }
}

