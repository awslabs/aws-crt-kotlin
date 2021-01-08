/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt.auth.signing

import software.amazon.awssdk.kotlin.crt.auth.credentials.Credentials
import software.amazon.awssdk.kotlin.crt.auth.credentials.CredentialsProvider

public enum class AwsSigningAlgorithm(public val value: Int) {
    SIGV4(0);
}

public enum class AwsSignatureType(public val value: Int) {
    HTTP_REQUEST_VIA_HEADERS(0),
    HTTP_REQUEST_VIA_QUERY_PARAMS(1),
    HTTP_REQUEST_CHUNK(2),
    HTTP_REQUEST_EVENT(3);
}

public object AwsSignedBodyValue {
    public const val EMPTY_SHA256: String = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    public const val UNSIGNED_PAYLOAD: String = "UNSIGNED-PAYLOAD"
    public const val STREAMING_AWS4_HMAC_SHA256_PAYLOAD: String = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD"
    public const val STREAMING_AWS4_HMAC_SHA256_EVENTS: String = "STREAMING-AWS4-HMAC-SHA256-EVENTS"
}

public enum class AwsSignedBodyHeaderType(public val value: Int) {
    /**
     * Do not add a header
     */
    NONE(0),

    /**
     * Add the "x-amz-content-sha256" header with the canonical request's body value
     */
    X_AMZ_CONTENT_SHA256(1);
}

/**
 * Predicate function used to determine if a specific header should be signed or not
 */
public typealias ShouldSignHeaderFn = (String) -> Boolean

/**
 * A configuration structure for use in AWS-related signing
 */
public class AwsSigningConfig(builder: Builder) {

    public companion object {
        public fun build(block: Builder.() -> Unit): AwsSigningConfig = Builder().apply(block).build()
    }

    /**
     * The region to sign against
     */
    public val region: String = requireNotNull(builder.region) { "signing config must specify a region" }
    /**
     * name of service to sign a request for
     */
    public val service: String = requireNotNull(builder.service) { "signing config must specify a service" }

    /**
     * Raw date (epoch milliseconds) to use during the signing process.
     */
    public val date: Long = builder.date ?: software.amazon.awssdk.kotlin.crt.Platform.epochMilliNow()

    /**
     * Optional function to control which headers are a part of the canonical request.
     * Skipping auth-required headers will result in an unusable signature.  Headers injected by the signing process
     * are not skippable.
     *
     * This function does not override the internal check function (x-amzn-trace-id, user-agent), but rather
     * supplements it.  In particular, a header will get signed if and only if it returns true to both
     * the internal check (skips x-amzn-trace-id, user-agent) and this function (if defined).
     */
    public val shouldSignHeader: ShouldSignHeaderFn? = builder.shouldSignHeader

    /**
     * What signing algorithm to use.
     */
    public val algorithm: AwsSigningAlgorithm = builder.algorithm

    /**
     * What sort of signature should be computed?
     */
    public val signatureType: AwsSignatureType = builder.signatureType

    /**
     * We assume the uri will be encoded once in preparation for transmission.  Certain services
     * do not decode before checking signature, requiring us to actually double-encode the uri in the canonical
     * request in order to pass a signature check.
     */
    public val useDoubleUriEncode: Boolean = builder.useDoubleUriEncode
    /**
     * Controls whether or not the uri paths should be normalized when building the canonical request
     */
    public val normalizeUriPath: Boolean = builder.normalizeUriPath
    /**
     * Should the "X-Amz-Security-Token" query param be omitted?
     * Normally, this parameter is added during signing if the credentials have a session token.
     * The only known case where this should be true is when signing a websocket handshake to IoT Core.
     */
    public val omitSessionToken: Boolean = builder.omitSessionToken

    /**
     * Optional string to use as the canonical request's body public value.
     * If string is empty, a public value will be calculated from the payload during signing.
     * Typically, this is the SHA-256 of the (request/chunk/event) payload, written as lowercase hex.
     * If this has been precalculated, it can be set here. Special public values used by certain services can also be set
     * (e.g. "UNSIGNED-PAYLOAD" "STREAMING-AWS4-HMAC-SHA256-PAYLOAD" "STREAMING-AWS4-HMAC-SHA256-EVENTS").
     */
    public val signedBodyValue: String? = builder.signedBodyValue

    /**
     * Controls what body "hash" header, if any, should be added to the canonical request and the signed request:
     *   AWS_SBHT_NONE - no header should be added
     *   AWS_SBHT_X_AMZ_CONTENT_SHA256 - the body "hash" should be added in the X-Amz-Content-Sha256 header
     */
    public val signedBodyHeader: AwsSignedBodyHeaderType = builder.signedBodyHeader

    /*
     * Signing key control:
     *
     *   (1) If "credentials" is public valid, use it
     *   (2) Else if "credentials_provider" is public valid, query credentials from the provider and use the result
     *   (3) Else fail
     *
     */

    /**
     * AWS Credentials to sign with.
     */
    public val credentials: Credentials? = builder.credentials

    /**
     * AWS credentials provider to fetch credentials from.
     */
    public val credentialsProvider: CredentialsProvider? = builder.credentialsProvider

    init {
        if (credentials == null && credentialsProvider == null) {
            throw IllegalArgumentException("signing config must specify one of `credentials` or `credentialsProvider`")
        }
    }

    /**
     * If non-zero and the signing transform is query param, then signing will add X-Amz-Expires to the query
     * string, equal to the public value specified here.  If this public value is zero or if header signing is being used then
     * this parameter has no effect.
     */
    public val expirationInSeconds: Long = builder.expirationInSeconds

    public class Builder {
        public var region: String? = null
        public var service: String? = null
        public var date: Long? = null
        public var algorithm: AwsSigningAlgorithm = AwsSigningAlgorithm.SIGV4
        public var shouldSignHeader: ShouldSignHeaderFn? = null
        public var signatureType: AwsSignatureType = AwsSignatureType.HTTP_REQUEST_VIA_HEADERS
        public var useDoubleUriEncode: Boolean = true
        public var normalizeUriPath: Boolean = true
        public var omitSessionToken: Boolean = false
        public var signedBodyValue: String? = null
        public var signedBodyHeader: AwsSignedBodyHeaderType = AwsSignedBodyHeaderType.NONE
        public var credentials: Credentials? = null
        public var credentialsProvider: CredentialsProvider? = null
        public var expirationInSeconds: Long = 0

        public fun build(): AwsSigningConfig = AwsSigningConfig(this)
    }
}
