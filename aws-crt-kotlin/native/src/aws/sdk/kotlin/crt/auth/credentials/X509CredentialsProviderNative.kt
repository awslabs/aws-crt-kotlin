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

public actual class X509CredentialsProvider internal actual constructor(builder: X509CredentialsProviderBuilder) :
    CredentialsProvider {
    public actual companion object {}

    private val shutdownCompleteChannel = Channel<Unit>(1)
    private val channelStableRef = StableRef.create(shutdownCompleteChannel)

    private val provider: aws_credentials_provider

    init {
        provider = memScoped {
            val tlsOpts = cValue<aws_tls_connection_options> {
                this.ctx = builder.tlsContext?.ptr
//                this.alpn_list =
//                this.on_error =
//                this.advertise_alpn_message =
//                this.on_data_read =
//                this.on_negotiation_result =
//                this.server_name =
//                this.timeout_ms =
//                this.user_data =
            }

//            val proxyOpts = cValue<cnames.structs.aws_http_proxy_options> {
//                this.
//            }

            val opts = cValue<aws_credentials_provider_x509_options> {
                shutdown_options.apply {
                    shutdown_callback = staticCFunction(::onShutdownComplete)
                    shutdown_user_data = channelStableRef.asCPointer()
                }
                bootstrap = builder.clientBootstrap?.ptr
                tls_connection_options = tlsOpts.ptr
                builder.thingName?.let { thing_name.initFromCursor(it.toAwsString().asAwsByteCursor()) }
                builder.roleAlias?.let { role_alias.initFromCursor(it.toAwsString().asAwsByteCursor()) }
                builder.endpoint?.let { endpoint.initFromCursor(it.toAwsString().asAwsByteCursor()) }
            }

            checkNotNull(aws_credentials_provider_new_x509(Allocator.Default.allocator, opts.ptr)) {
                "aws_credentials_provider_new_x509"
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
