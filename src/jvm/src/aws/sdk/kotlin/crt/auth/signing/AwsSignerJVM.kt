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

public actual object AwsSigner {
    public actual suspend fun signRequest(request: HttpRequest, config: AwsSigningConfig): HttpRequest {
        // FIXME - this would be a good area where talking directly to JNI would be convenient so we don't have to
        // do [KotlinHttpReq -> CrtJava -> Native] and back
        val jniReq = request.into()
        return asyncCrtJniCall {
            val reqFuture = AwsSignerJni.signRequest(jniReq, config.into())
            val signedJniReq = reqFuture.await()
            HttpRequest.from(signedJniReq)
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
