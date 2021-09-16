package aws.sdk.kotlin.crt.auth.signing

import aws.sdk.kotlin.crt.http.HttpRequest

/**
 * Wrapper that holds signing-related output.  Depending on the signing configuration, not all members may be
 * assigned and some members, like signature, may have a variable format.
 */
public data class AwsSigningResult(
    /**
     * The signed HTTP request from the result, may be null if an http request was not signed
     */
    val signedRequest: HttpRequest?,

    /**
     * Gets the signature value from the result.  Depending on the requested signature type and algorithm, this value
     * will be in one of the following formats:
     *
     *   (1) HTTP_REQUEST_VIA_HEADERS - hex encoding of the binary signature value
     *   (2) HTTP_REQUEST_VIA_QUERY_PARAMS - hex encoding of the binary signature value
     *   (3) HTTP_REQUEST_CHUNK/SIGV4 - hex encoding of the binary signature value
     *   (4) HTTP_REQUEST_CHUNK/SIGV4_ASYMMETRIC - '*'-padded hex encoding of the binary signature value
     *   (5) HTTP_REQUEST_EVENT - binary signature value (NYI)
     *
     * @return the signature value from the signing process
     */
    val signature: ByteArray?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AwsSigningResult

        if (signedRequest != other.signedRequest) return false
        if (signature != null) {
            if (other.signature == null) return false
            if (!signature.contentEquals(other.signature)) return false
        } else if (other.signature != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = signedRequest?.hashCode() ?: 0
        result = 31 * result + (signature?.contentHashCode() ?: 0)
        return result
    }
}
