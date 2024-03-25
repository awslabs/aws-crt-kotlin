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
    expiration_timepoint_seconds = UINT64_MAX // FIXME?: Our Credentials do not have an expiration field
)

/**
 * Convert a Kotlin [CredentialsProvider] to a delegate [aws_credentials_provider]
 */
internal fun CredentialsProvider.toNativeCredentialsProvider(): CValue<aws_credentials_provider> {
//    fun nativeGetCredentialsFn(
//        provider: CPointer<aws_credentials_provider>?,
//        callback: CPointer<aws_on_get_credentials_callback_fn>?,
//        userData: COpaquePointer?
//    ): Int {
//        val kProvider = this
//        val kCredentials = runBlocking { kProvider.getCredentials() }
//        val nativeCredentials = kCredentials.toNativeCredentials()
//        callback?.invoke(nativeCredentials, 0, userData)
//        return 0
//    }

//    fun nativeDestroyFn(provider: CPointer<aws_credentials_provider>?) { close() }

//    val vTable = Allocator.Default.alloc<s_crt_kotlin_aws_credentials_provider_vtable> {
//        this.get_credentials = staticCFunction(::nativeGetCredentialsFn)
//        this.destroy = staticCFunction(::nativeDestroyFn)
//    }

//    val provider = cValue<s_crt_kotlin_aws_credentials_provider> {
//        this.vtable = vTable.ptr
//        this.allocator = Allocator.Default.allocator
//        this.
//    }

//    return provider

    // START delegate implementation

    // TODO Need shutdown options?
//    val shutdownOptions = cValue<aws_credentials_provider_shutdown_options> {
//        this.shutdown_callback = staticCFunction(::onShutdownComplete)
//        this.shutdown_user_data = channelStableRef.asCPointer()
//    }

    fun cValueGetCredentialsDelegate(
        delegateUserData: COpaquePointer?,
        callback: CValue<aws_on_get_credentials_callback_fn>,
        callbackUserData: COpaquePointer?
    ): Int {
        return 0
    }

    fun getCredentialsDelegate(
        delegateUserData: COpaquePointer?,
        callback: CPointer<aws_on_get_credentials_callback_fn>,
        callbackUserData: COpaquePointer?
    ): Int {
        return 0
    }

    val providerStableRef = StableRef.create(this)

    val myOptions = cValue<aws_credentials_provider_delegate_options> {
        this.get_credentials = staticCFunction(::getCredentialsDelegate)
        this.delegate_user_data = providerStableRef.asCPointer()
//        this.shutdown_options =
    }

//
//    val options = cValue<aws_credentials_provider_delegate_options> {
//        this.get_credentials = staticCFunction(::getCredentialsDelegate)
////        this.delegate_user_data =
////        this.shutdown_options =
//    }

    val provider = aws_credentials_provider_new_delegate()

}


//    val getCredentialsFn = staticCFunction(s_crt_kotlin_aws_credentials_provider_get_credentials)
//
////    val vtable = cValue<s_crt_kotlin_aws_credentials_provider_vtable> {}
//
////    val nativeProvider = cValue<aws_credentials_provider> {
//        // this.vtable =
////        this.allocator = Allocator.Default.allocator
//        // this.shutdown_options =
//        // this.impl =
//        // this.ref_count =
////    }
//
//    return null
//}
//
//internal fun nativeGetCredentials(kGetCredentials: suspend () -> Credentials): CPointer<CFunction<*>> {
//
//}
