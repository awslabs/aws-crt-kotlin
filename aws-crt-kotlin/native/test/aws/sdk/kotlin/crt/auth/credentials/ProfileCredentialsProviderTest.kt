/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt.auth.credentials

import aws.sdk.kotlin.crt.CRT
import aws.sdk.kotlin.crt.CrtTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import platform.posix.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class ProfileCredentialsProviderTest : CrtTest() {
    private val CONFIG_FILE_PATH = "aws_sdk_kotlin_test_config"
    private val CREDENTIALS_FILE_PATH = "aws_sdk_kotlin_test_credentials"
    private val TEST_CREDENTIALS = Credentials("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", null)

    @BeforeTest
    fun setup() {
        // create an empty config file
        val configFile = checkNotNull(fopen(CONFIG_FILE_PATH, "w")) {
            "fopen(): ${CRT.lastError()}"
        }
        val configContent = """
            [profile matas]
            """.trimIndent()
        check(fputs(configContent, configFile) >= 0) { "fputs(): ${CRT.lastError()}" }
        check(fclose(configFile) == 0) { "fclose(): ${CRT.lastError()}" }

        // create a credentials file, fill it with test credentials
        val credentialsFile = checkNotNull(fopen(CREDENTIALS_FILE_PATH, "w")) {
            "fopen(): ${CRT.lastError()}"
        }
        val credentialsContent = """
            [foobar]
            aws_access_key_id = ${TEST_CREDENTIALS.accessKeyId}
            aws_secret_access_key = ${TEST_CREDENTIALS.secretAccessKey}
        """.trimIndent()
        check(fputs(credentialsContent, configFile) >= 0) { "fputs(): ${CRT.lastError()}" }
        check(fclose(credentialsFile) == 0) { "fclose(): ${CRT.lastError()}" }
    }

    @AfterTest
    fun cleanup() {
        remove(CONFIG_FILE_PATH)
        remove(CREDENTIALS_FILE_PATH)
    }

    @Test
    fun testCreateProvider() = runTest {
        val builder = ProfileCredentialsProviderBuilder()
        builder.apply {
            configFileName = CONFIG_FILE_PATH
            credentialsFileName = CREDENTIALS_FILE_PATH
            profileName = "foobar"
        }

        val provider = ProfileCredentialsProvider(builder)
        provider.close()
        provider.waitForShutdown()
    }

    @Ignore // Segfault in `aws_profile_collection_new_from_merge` invoked during getCredentials
    @Test
    fun testGetCredentials() = runTest {
        val builder = ProfileCredentialsProviderBuilder()
        builder.apply {
            configFileName = CONFIG_FILE_PATH
            credentialsFileName = CREDENTIALS_FILE_PATH
            profileName = "foobar"
        }

        val provider = ProfileCredentialsProvider(builder)
        assertEquals(TEST_CREDENTIALS, provider.getCredentials())

        provider.close()
        provider.waitForShutdown()
    }
}
