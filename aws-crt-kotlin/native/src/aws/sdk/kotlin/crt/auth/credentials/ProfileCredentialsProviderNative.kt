/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.Allocator
import aws.sdk.kotlin.crt.awsAssertOpSuccess
import aws.sdk.kotlin.crt.util.asAwsByteCursor
import aws.sdk.kotlin.crt.util.initFromCursor
import aws.sdk.kotlin.crt.util.toAwsString
import aws.sdk.kotlin.crt.util.toKString
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import libcrt.*

public actual class ProfileCredentialsProvider
internal actual constructor(builder: ProfileCredentialsProviderBuilder) :
    CredentialsProvider {
    public actual companion object {}

    private val shutdownCompleteChannel = Channel<Unit>(1)
    private val channelStableRef = StableRef.create(shutdownCompleteChannel)

    private val provider: aws_credentials_provider

    init {
        provider = memScoped {
            val opts = cValue<aws_credentials_provider_profile_options> {
                bootstrap = builder.clientBootstrap?.ptr
                builder.configFileName?.let { config_file_name_override.initFromCursor(it.toAwsString().asAwsByteCursor()) }
                builder.credentialsFileName?.let { credentials_file_name_override.initFromCursor(it.toAwsString().asAwsByteCursor()) }
                builder.profileName?.let { profile_name_override.initFromCursor(it.toAwsString().asAwsByteCursor()) }
                shutdown_options.apply {
                    shutdown_callback = staticCFunction(::onShutdownComplete)
                    shutdown_user_data = channelStableRef.asCPointer()
                }
                tls_ctx = builder.tlsContext?.ptr
            }

            checkNotNull(aws_credentials_provider_new_profile(Allocator.Default.allocator, opts.ptr)) {
                "aws_credentials_provider_new_profile"
            }.pointed
        }
    }

    override suspend fun getCredentials(): Credentials {
        val credentialsStableRef = StableRef.create(Channel<Credentials>(Channel.RENDEZVOUS))

        awsAssertOpSuccess(s_crt_kotlin_aws_credentials_provider_get_credentials(
            provider = provider.ptr,
            callback = staticCFunction(::getCredentialsCallback),
            user_data = credentialsStableRef.asCPointer()
        )) { "aws_credentials_provider_get_credentials()" }

        return credentialsStableRef.get().receive()
    }

    override fun close() {
        aws_credentials_provider_release(provider.ptr)
    }

    override suspend fun waitForShutdown() {
        shutdownCompleteChannel.receive()
        shutdownCompleteChannel.close()
        channelStableRef.dispose()
    }
}
