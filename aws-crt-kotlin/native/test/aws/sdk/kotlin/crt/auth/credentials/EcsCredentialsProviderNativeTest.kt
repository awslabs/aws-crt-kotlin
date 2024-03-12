/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class EcsCredentialsProviderTest {
    @Test
    fun testCreateProvider() = runTest {
        val provider = EcsCredentialsProvider(EcsCredentialsProviderBuilder())
        provider.close()
        provider.waitForShutdown()
    }
}