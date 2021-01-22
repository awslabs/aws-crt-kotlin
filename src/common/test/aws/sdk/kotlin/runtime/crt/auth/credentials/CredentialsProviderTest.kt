/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt.auth.credentials

import aws.sdk.kotlin.runtime.crt.CrtTest
import aws.sdk.kotlin.runtime.crt.io.ClientBootstrap
import aws.sdk.kotlin.runtime.crt.io.EventLoopGroup
import aws.sdk.kotlin.runtime.crt.io.HostResolver
import aws.sdk.kotlin.runtime.crt.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CredentialsProviderTest : CrtTest() {
    private val EXPECTED_CREDENTIALS = Credentials("access_key_id", "secret_access_key", "session_token")

    @Test
    fun testStaticProvider() = runSuspendTest {
        val provider = StaticCredentialsProvider.fromCredentials(EXPECTED_CREDENTIALS)
        val actual = provider.getCredentials()
        assertEquals(EXPECTED_CREDENTIALS, actual)
    }

    @Test
    fun testCreateDestroyDefaultChain() {
        val elg = EventLoopGroup(1)
        val hr = HostResolver(elg)
        val bootstrap = ClientBootstrap(elg, hr)
        try {
            val provider = DefaultChainCredentialsProvider.build {
                clientBootstrap = bootstrap
            }
        } finally {
            bootstrap.close()
            hr.close()
            elg.close()
        }
    }

    @Test
    fun testCacheStatic() = runSuspendTest {
        val provider = CachedCredentialsProvider.build {
            source = StaticCredentialsProvider.fromCredentials(EXPECTED_CREDENTIALS)
            refreshTimeInMilliseconds = 900
        }

        val actual = provider.getCredentials()
        assertEquals(EXPECTED_CREDENTIALS, actual)
    }
}
