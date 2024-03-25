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
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import libcrt.*

public actual class StsAssumeRoleCredentialsProvider
internal actual constructor(builder: StsAssumeRoleCredentialsProviderBuilder) : CredentialsProvider {
    public actual companion object {}

    private val shutdownCompleteChannel = Channel<Unit>(Channel.RENDEZVOUS)
    private val channelStableRef = StableRef.create(shutdownCompleteChannel)

    private val provider: aws_credentials_provider

    init {
        provider = memScoped {
            val opts = cValue<aws_credentials_provider_sts_options> {
                bootstrap = builder.clientBootstrap?.ptr
                creds_provider = builder.credentialsProvider?.toNativeCredentialsProvider()?.ptr?.reinterpret() // FIXME is this .reinterpret() correct?
                duration_seconds = builder.durationSeconds!!.convert()
                role_arn.initFromCursor(builder.roleArn!!.toAwsString().asAwsByteCursor())
                session_name.initFromCursor(builder.sessionName!!.toAwsString().asAwsByteCursor())
                shutdown_options.apply {
                    shutdown_callback = staticCFunction(::onShutdownComplete)
                    shutdown_user_data = channelStableRef.asCPointer()
                }
                tls_ctx = builder.tlsContext?.ptr
            }

            checkNotNull(aws_credentials_provider_new_sts(Allocator.Default.allocator, opts.ptr)) {
                "aws_credentials_provider_new_sts()"
            }.pointed
        }
    }


    override suspend fun getCredentials(): Credentials {
        val credentialsStableRef = StableRef.create(Channel<Credentials>(Channel.RENDEZVOUS))

        awsAssertOpSuccess(s_crt_kotlin_aws_credentials_provider_get_credentials(
            provider = provider.ptr,
            callback = staticCFunction(::getCredentialsCallback),
            user_data = credentialsStableRef.asCPointer()

        )) { "aws_credentials_provider_sts->aws_credentials_provider_get_credentials()" }

        return credentialsStableRef.get().receive()
    }

    override fun close() {
        aws_credentials_provider_release(provider.ptr)
    }

    override suspend fun waitForShutdown() {
        shutdownCompleteChannel.tryReceive()
        shutdownCompleteChannel.close()
        channelStableRef.dispose()
    }
}