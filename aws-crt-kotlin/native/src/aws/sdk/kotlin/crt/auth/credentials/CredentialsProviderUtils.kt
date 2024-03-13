/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.CRT
import aws.sdk.kotlin.crt.awsAssertOpSuccess
import aws.sdk.kotlin.crt.util.toAwsString
import aws.sdk.kotlin.crt.util.toKString
import cnames.structs.aws_credentials
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import libcrt.*
import platform.posix.UINT64_MAX

/**
 * Callback function used in asynchronous getCredentials requests.
 * @param credentialsPtr A pointer to the [aws_credentials], which may be null if there was an error.
 * @param errorCode The error code associated with credentials resolution
 * @param userData The user data which was passed to CRT
 */
internal fun getCredentialsCallback(credentialsPtr: CPointer<aws_credentials>?, errorCode: Int, userData: COpaquePointer?) {
    awsAssertOpSuccess(errorCode) { "getCredentials failed: ${CRT.lastError()}" }
    checkNotNull(userData) { "aws_credentials_provider_get_credentials() received null userData" }
    val credentialsStableRef = userData.asStableRef<Channel<Credentials>>()

    memScoped {
        val accessKeyId = aws_credentials_get_access_key_id(credentialsPtr).ptr.pointed.toKString()
        val secretAccessKey = aws_credentials_get_secret_access_key(credentialsPtr).ptr.pointed.toKString()
        val sessionToken = aws_credentials_get_session_token(credentialsPtr).ptr.pointed.toKString().takeIf { it.isNotBlank() }

        runBlocking {
            credentialsStableRef.get().send(Credentials(accessKeyId, secretAccessKey, sessionToken))
        }
    }
}

/**
 * Function passed to CRT in shutdown_opts used to indicate the provider is closed
 */
internal fun onShutdownComplete(userData: COpaquePointer?) {
    if (userData != null) {
        val shutdownCompleteChannel = userData.asStableRef<Channel<Unit>>().get()
        shutdownCompleteChannel.trySend(Unit)
    }
}

internal fun Credentials.toNativeCredentials(): CPointer<aws_credentials>? = aws_credentials_new_from_string(
    Allocator.Default.allocator,
    access_key_id = accessKeyId.toAwsString(),
    secret_access_key = secretAccessKey.toAwsString(),
    session_token = sessionToken?.toAwsString(),
    expiration_timepoint_seconds = UINT64_MAX // FIXME: Credentials do not have an expiration field
)
