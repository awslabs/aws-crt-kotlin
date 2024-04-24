/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.signing

import aws.sdk.kotlin.crt.*
import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.auth.credentials.Credentials
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
import platform.posix.UINT64_MAX

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
    ): AwsSigningResult = memScoped {
        val nativeRequest = request.toNativeRequest().pin()
        println("lauzadis: done making a native request")

        // Pair of HTTP request and callback channel containing the signature
        val userData = nativeRequest to Channel<ByteArray>(1)
        val userDataStableRef = StableRef.create(userData)
        println("lauzadis: done making user data")

        val signable = checkNotNull(aws_signable_new_http_request(
            allocator = Allocator.Default.allocator,
            request = nativeRequest.get()
        )) { "aws_signable_new_http_request" }
        println("lauzadis: done with aws_signable_new_http_request")

        val nativeSigningConfig: CPointer<aws_signing_config_base> = config.toNativeSigningConfig().reinterpret()
        println("lauzadis: done making native signing config")

        awsAssertOpSuccess(aws_sign_request_aws(
            allocator = Allocator.Default.allocator,
            signable = signable,
            base_config = nativeSigningConfig,
            on_complete = staticCFunction(::signCallback),
            userdata = userDataStableRef.asCPointer(),
        )) { "sign() aws_sign_request_aws" }
        println("lauzadis: done signing")

        val callbackChannel = userDataStableRef.get().second
        println("lauzadis: waiting for async signing callback")
        val signature = runBlocking { callbackChannel.receive() } // wait for async signing to complete....
        println("lauzadis: done waiting for async signing callback")

        val signedRequest = userDataStableRef.get().first
        return AwsSigningResult(signedRequest.get().toHttpRequest(), signature)
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
    // https://github.com/awslabs/aws-c-auth/blob/0d2aa00ae70c699fcb14d0338c1b07a58b9eb24b/include/aws/auth/signing.h#L26-L27
    val signature = Allocator.Default.allocPointerTo<aws_string>()
    val propertyName = "signature".toAwsString()

    awsAssertOpSuccess(aws_signing_result_get_property(this, propertyName, signature.ptr)) {
        "aws_signing_result_get_property"
    }

    val kSignature = signature.value?.toKString()?.encodeToByteArray()
    println("signature: ${kSignature?.decodeToString()}")
    return checkNotNull(kSignature) { "signature was null" }
}

private fun AwsSigningAlgorithm.toNativeSigningAlgorithm(): aws_signing_algorithm = when (this) {
    AwsSigningAlgorithm.SIGV4 -> aws_signing_algorithm.AWS_SIGNING_ALGORITHM_V4
    AwsSigningAlgorithm.SIGV4_ASYMMETRIC -> aws_signing_algorithm.AWS_SIGNING_ALGORITHM_V4_ASYMMETRIC
}

private fun AwsSignatureType.toNativeSignatureType(): aws_signature_type = when (this) {
    AwsSignatureType.HTTP_REQUEST_VIA_HEADERS -> aws_signature_type.AWS_ST_HTTP_REQUEST_HEADERS
    AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS -> aws_signature_type.AWS_ST_HTTP_REQUEST_QUERY_PARAMS
    AwsSignatureType.HTTP_REQUEST_EVENT -> aws_signature_type.AWS_ST_HTTP_REQUEST_EVENT
    AwsSignatureType.HTTP_REQUEST_CHUNK -> aws_signature_type.AWS_ST_HTTP_REQUEST_CHUNK
    AwsSignatureType.HTTP_REQUEST_TRAILING_HEADERS -> aws_signature_type.AWS_ST_HTTP_REQUEST_TRAILING_HEADERS
}

private fun AwsSignedBodyHeaderType.toNativeSignedBodyHeaderType() = when (this) {
    AwsSignedBodyHeaderType.NONE -> aws_signed_body_header_type.AWS_SBHT_NONE
    AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256 -> aws_signed_body_header_type.AWS_SBHT_X_AMZ_CONTENT_SHA256
}

private fun AwsSigningConfig.toNativeSigningConfig(): CPointer<aws_signing_config_aws> {
    val config = Allocator.Default.alloc<aws_signing_config_aws>()

    config.apply {
        config_type = AWS_SIGNING_CONFIG_AWS
        algorithm = this@toNativeSigningConfig.algorithm.toNativeSigningAlgorithm()
        signature_type = this@toNativeSigningConfig.signatureType.toNativeSignatureType()
        region.initFromCursor(this@toNativeSigningConfig.region.toAwsString().asAwsByteCursor())
        service.initFromCursor(this@toNativeSigningConfig.service.toAwsString().asAwsByteCursor())
        aws_date_time_init_epoch_millis(date.ptr, this@toNativeSigningConfig.date.toULong())

        // FIXME Can't convert Kotlin config's shouldSignHeader to a C function without capturing the config variable, and staticCFunction cannot capture variables.
        // should_sign_header = this@toNativeSigningConfig.shouldSignHeader?.toNativeShouldSignHeaderFn()
        // should_sign_header_ud =

        flags.use_double_uri_encode = if (this@toNativeSigningConfig.useDoubleUriEncode) 1u else 0u
        flags.should_normalize_uri_path = if (this@toNativeSigningConfig.normalizeUriPath) 1u else 0u
        flags.omit_session_token = if (this@toNativeSigningConfig.omitSessionToken) 1u else 0u

        this@toNativeSigningConfig.signedBodyValue?.let {
            signed_body_value.initFromCursor(it.toAwsString().asAwsByteCursor())
        }
        signed_body_header = this@toNativeSigningConfig.signedBodyHeader.toNativeSignedBodyHeaderType()

        credentials = this@toNativeSigningConfig.credentials?.toNativeCredentials()
        // TODO implement native CredentialsProvider
        // credentials_provider =

        expiration_in_seconds = this@toNativeSigningConfig.expirationInSeconds.toULong()
    }

    return config.ptr
}

//private fun ShouldSignHeaderFn.toNativeShouldSignHeaderFn(): CPointer<aws_should_sign_header_fn> =
//    staticCFunction { byteCursor, _  ->
//        val kString = byteCursor?.pointed?.toKString()
//        kString?.let { this(kString) } ?: false
//    }

/**
 * Gets called by the signing function when the signing is complete.
 *
 * Note that result will be destroyed after this function returns, so either copy it,
 * or do all necessary adjustments inside the callback.
 *
 * When performing event or chunk signing, you will need to copy out the signature value in order
 * to correctly configure the signable that wraps the event or chunk you want signed next.  The signature is
 * found in the "signature" property on the signing result.  This value must be added as the
 * "previous-signature" property on the next signable.
 */
private fun signCallback(signingResult: CPointer<aws_signing_result>?, errorCode: Int, userData: COpaquePointer?) {
    log(LogLevel.Trace, "lauzadis: in signCallback")
    println("lauzadis: in signCallback")
    awsAssertOpSuccess(errorCode) { "signing failed with code $errorCode: ${CRT.errorString(errorCode)}" }
    checkNotNull(signingResult) { "signing callback received null aws_signing_result" }
    checkNotNull(userData) { "signing callback received null user data"}

    val (requestToSign, callbackChannel) = userData
        .asStableRef<Pair<Pinned<CPointer<cnames.structs.aws_http_message>>, Channel<ByteArray>>>()
        .get()
    log(LogLevel.Trace, "got requestToSign and callbackChannel from user data")

    log(LogLevel.Trace, "lauzadis: calling aws_apply_signing_result_to_http_request on requestToSign: ${requestToSign.get()}")
    awsAssertOpSuccess(aws_apply_signing_result_to_http_request(requestToSign.get(), Allocator.Default.allocator, signingResult)) {
        "aws_apply_signing_result_to_http_request"
    }
    log(LogLevel.Trace, "lauzadis: done calling aws_apply_signing_result_to_http_request on requestToSign: ${requestToSign.get()}")

    val signedPath = cValue<aws_byte_cursor>()
    aws_http_message_get_request_path(requestToSign.get(), signedPath)
    log(LogLevel.Trace, "lauzadis: signed path: ${signedPath.toKString()}")

    log(LogLevel.Trace, "lauzadis: sending signature to channel")
    runBlocking {
        callbackChannel.send(signingResult.getSignature())
    }
}

internal fun Credentials.toNativeCredentials(): CPointer<cnames.structs.aws_credentials>? = aws_credentials_new_from_string(
    Allocator.Default.allocator,
    access_key_id = accessKeyId.toAwsString(),
    secret_access_key = secretAccessKey.toAwsString(),
    session_token = sessionToken?.toAwsString(),
    expiration_timepoint_seconds = UINT64_MAX // FIXME?: Our Credentials do not have an expiration field
)

private fun CPointer<aws_signing_result>.getProperties(property: String): Map<String, String> = memScoped {
    val outList = Allocator.Default.allocPointerTo<aws_array_list>()
    aws_signing_result_get_property_list(this@getProperties, property.toAwsString(), outList.ptr)

    val map: MutableMap<String, String> = mutableMapOf()

    for (i in 0 until aws_array_list_length(outList.value).convert()) {
        val property = Allocator.Default.alloc<aws_signing_result_property>()
        aws_array_list_get_at(outList.value, property.ptr, i.convert())
        map[property.name!!.toKString()!!] = property.value!!.toKString()!!
    }

    return map
}

private fun CPointer<aws_signing_result>.printProperties(property: String) {
    val props = getProperties(property)
    log(LogLevel.Trace, "signing result contains ${props.size} $property")
    props.forEach {
        log(LogLevel.Trace, "\t${it.key}=${it.value}")
    }
}