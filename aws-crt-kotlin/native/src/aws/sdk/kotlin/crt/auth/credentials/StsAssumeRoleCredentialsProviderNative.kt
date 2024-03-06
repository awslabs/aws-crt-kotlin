/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.util.asAwsByteCursor
import aws.sdk.kotlin.crt.util.free
import aws.sdk.kotlin.crt.util.toAwsString
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import libcrt.*

@OptIn(ExperimentalForeignApi::class)
public actual class StsAssumeRoleCredentialsProvider
internal actual constructor(builder: StsAssumeRoleCredentialsProviderBuilder) : CredentialsProvider {
    public actual companion object {}

    private val shutdownCompleteChannel = Channel<Unit>(Channel.RENDEZVOUS)
    private val channelStableRef = StableRef.create(shutdownCompleteChannel)

//    private val provider: cnames.structs.aws_credentials_provider

    init {
        memScoped {
            val shutdownOpts = cValue<aws_credentials_provider_shutdown_options>().apply {
                shutdown_callback = staticCFunction(::onShutdownComplete)
                shutdown_user_data = channelStableRef.asCPointer()
            }
        }
//        val roleArn = builder.roleArn!!.toAwsString()
//        val sessionName = builder.sessionName!!.toAwsString()
//
//        val opts = cValue<aws_credentials_provider_sts_options> {
//            aws_client_bootstrap = builder.clientBootstrap?.ptr
//            aws_tls_ctx = builder.tlsContext?.ptr
//            aws_credentials_provider = builder.credentialsProvider?.ptr
//            role_arn = roleArn.asAwsByteCursor()
//            session_name = sessionName.asAwsByteCursor()
//            duration_seconds = builder.durationSeconds?.convert()
//            shutdown_options = shutdownOpts
//        }
//
//        provider = aws_credentials_provider_new_sts(Allocator.Default.allocator, opts)
//
//        roleArn.free()
//        sessionName.free()
    }


    override suspend fun getCredentials(): Credentials {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override suspend fun waitForShutdown() {
        TODO("Not yet implemented")
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun onShutdownComplete(userData: COpaquePointer?) {
    if (userData != null) {
        val shutdownCompleteChannel = userData.asStableRef<Channel<Unit>>().get()
        shutdownCompleteChannel.trySend(Unit)
        shutdownCompleteChannel.close()
    }
}
