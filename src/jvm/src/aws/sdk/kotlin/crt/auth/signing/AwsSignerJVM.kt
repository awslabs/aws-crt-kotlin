/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt.auth.signing

import aws.sdk.kotlin.crt.asyncCrtJniCall
import aws.sdk.kotlin.crt.auth.credentials.JniCredentialsProvider
import aws.sdk.kotlin.crt.http.HttpRequest
import aws.sdk.kotlin.crt.http.from
import aws.sdk.kotlin.crt.http.into
import kotlinx.coroutines.future.await
import software.amazon.awssdk.crt.auth.credentials.Credentials
import java.util.function.Predicate
import software.amazon.awssdk.crt.auth.signing.AwsSigner as AwsSignerJni
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig as AwsSigningConfigJni
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignatureType as AwsSignatureTypeJni
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyHeaderType as AwsSignedBodyHeaderTypeJni
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSigningAlgorithm as AwsSigningAlgorithmJni

/**
 * Static class for a variety of AWS signing APIs.
 */
public actual object AwsSigner {

    /**
     * Signs an http request according to the supplied signing configuration
     * @param request http request to sign
     * @param config signing configuration
     * @return signed request
     */
    public actual suspend fun signRequest(request: HttpRequest, config: AwsSigningConfig): HttpRequest =
        checkNotNull(sign(request, config).signedRequest) { "AwsSigningResult request must not be null" }

    /**
     * Signs an http request according to the supplied signing configuration
     * @param request http request to sign
     * @param config signing configuration
     * @return signing result, which provides access to all signing-related result properties
     */
    public actual suspend fun sign(request: HttpRequest, config: AwsSigningConfig): AwsSigningResult {
        // FIXME - this would be a good area where talking directly to JNI would be convenient so we don't have to
        // do [KotlinHttpReq -> CrtJava -> Native] and back
        val jniReq = request.into()
        return asyncCrtJniCall {
            val reqFuture = AwsSignerJni.sign(jniReq, config.into())
            val jniResult = reqFuture.await()
            val signedRequest = HttpRequest.from(jniResult.signedRequest)
            AwsSigningResult(signedRequest, jniResult.signature)
        }
    }
}

private fun AwsSigningConfig.into(): AwsSigningConfigJni {
    val ktConfig = this
    return AwsSigningConfigJni().apply {
        algorithm = AwsSigningAlgorithmJni.getEnumValueFromInteger(ktConfig.algorithm.value)
        signatureType = AwsSignatureTypeJni.getEnumValueFromInteger(ktConfig.signatureType.value)
        region = ktConfig.region
        service = ktConfig.service
        time = ktConfig.date
        ktConfig.credentials?.let {
            credentials = Credentials(
                it.accessKeyId.toByteArray(),
                it.secretAccessKey.toByteArray(),
                it.sessionToken?.toByteArray()
            )
        }

        val ktCredentialsProvider = ktConfig.credentialsProvider
        if (ktCredentialsProvider is JniCredentialsProvider) {
            // FIXME - crt-java requires inheriting from an abstract base and providing a native handle
            // which means you can't implement a credentials provider in pure Java/Kotlin
            credentialsProvider = ktCredentialsProvider.jniCredentials
        }

        val shouldSignHeaderFn = ktConfig.shouldSignHeader
        if (shouldSignHeaderFn != null) {
            shouldSignHeader = Predicate { shouldSignHeaderFn(it) }
        }
        useDoubleUriEncode = ktConfig.useDoubleUriEncode
        shouldNormalizeUriPath = ktConfig.normalizeUriPath
        omitSessionToken = ktConfig.omitSessionToken
        signedBodyHeader = AwsSignedBodyHeaderTypeJni.getEnumValueFromInteger(ktConfig.signedBodyHeader.value)
        signedBodyValue = ktConfig.signedBodyValue
        expirationInSeconds = ktConfig.expirationInSeconds
    }
}
