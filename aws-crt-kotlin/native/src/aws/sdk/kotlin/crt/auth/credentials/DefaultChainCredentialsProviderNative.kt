/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.awsAssertOpSuccess
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import libcrt.aws_credentials_provider
import libcrt.aws_credentials_provider_chain_options
import libcrt.aws_credentials_provider_release
import libcrt.s_crt_kotlin_aws_credentials_provider_get_credentials

public actual class DefaultChainCredentialsProvider internal actual constructor(builder: DefaultChainCredentialsProviderBuilder) :
    CredentialsProvider {
    public actual companion object {}

    private val shutdownCompleteChannel = Channel<Unit>(1)
    private val channelStableRef = StableRef.create(shutdownCompleteChannel)



//    private val provider: aws_credentials_provider
    init {
//        val provider = memScoped {
// TODO Build the default chain
//            val chainProviders: CPointer<CPointerVar<aws_credentials_provider>>
//            val opts = cValue<aws_credentials_provider_chain_options> {
//                shutdown_options.apply {
//                    shutdown_callback = staticCFunction(::onShutdownComplete)
//                    shutdown_user_data = channelStableRef.asCPointer()
//                }
//                providers = chainProviders
//            }
//        }
    }

    override suspend fun getCredentials(): Credentials {
        TODO("Not yet implemented")
//        val credentialsStableRef = StableRef.create(Channel<Credentials>(Channel.RENDEZVOUS))
//
//        awsAssertOpSuccess(
//            s_crt_kotlin_aws_credentials_provider_get_credentials(
//                provider = provider.ptr,
//                callback = staticCFunction(::getCredentialsCallback),
//                user_data = credentialsStableRef.asCPointer()
//            )
//        ) { "aws_credentials_provider_get_credentials()" }
//
//        return credentialsStableRef.get().receive()
    }

    override fun close() {
        TODO("Not yet implemented")
//        aws_credentials_provider_release(provider.ptr)
    }

    override suspend fun waitForShutdown() {
        TODO("Not yet implemented")
//        shutdownCompleteChannel.receive()
//        shutdownCompleteChannel.close()
//        channelStableRef.dispose()
    }
}
