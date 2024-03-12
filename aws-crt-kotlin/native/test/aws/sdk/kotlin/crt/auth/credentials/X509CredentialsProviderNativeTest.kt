/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.io.TlsContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class X509CredentialsProviderTest {
    @Test
    fun testCreateProvider() = runTest {
        val builder = X509CredentialsProviderBuilder()
        builder.apply {
            tlsContext = TlsContext()
            thingName = "thingy"
            roleAlias = "roley"
        }

        val provider = X509CredentialsProvider(builder)
        provider.close()
        provider.waitForShutdown()
    }
}