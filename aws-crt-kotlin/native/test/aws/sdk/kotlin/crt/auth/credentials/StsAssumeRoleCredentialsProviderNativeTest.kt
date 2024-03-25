import aws.sdk.kotlin.crt.CrtTest
import aws.sdk.kotlin.crt.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.crt.auth.credentials.StaticCredentialsProviderBuilder
import aws.sdk.kotlin.crt.auth.credentials.StsAssumeRoleCredentialsProvider
import aws.sdk.kotlin.crt.auth.credentials.StsAssumeRoleCredentialsProviderBuilder
import aws.sdk.kotlin.crt.io.ClientBootstrap
import aws.sdk.kotlin.crt.io.EventLoopGroup
import aws.sdk.kotlin.crt.io.HostResolver
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class StsAssumeRoleCredentialsProviderNativeTest : CrtTest() {

    private val bootstrapProvider = StaticCredentialsProviderBuilder().apply {
        accessKeyId = "accessKeyId"
        secretAccessKey = "secretAccessKey"
        sessionToken = "sessionToken"
    }.build()

    private lateinit var elg: EventLoopGroup
    private lateinit var hr: HostResolver
    private lateinit var cb: ClientBootstrap

    @BeforeTest
    fun setup() {
        elg = EventLoopGroup()
        hr = HostResolver(elg)
        cb = ClientBootstrap(elg, hr)
    }

    @AfterTest
    fun close() {
        cb.close()
        hr.close()
        elg.close()
    }

    fun testCreateDestroy() = runTest {
        val provider = StsAssumeRoleCredentialsProviderBuilder().apply {
            this.credentialsProvider = bootstrapProvider
            this.clientBootstrap = cb
            this.roleArn = "roleArn"
            this.sessionName = "sessionName"
            this.durationSeconds = 60
        }.build()

        provider.close()
    }

    fun testGetCredentials() = runTest {
        val provider = StsAssumeRoleCredentialsProviderBuilder().apply {
            this.credentialsProvider = bootstrapProvider
            this.clientBootstrap = cb
            this.roleArn = "arn:aws:iam::164711280977:role/S3ReadOnlyTestRole"
            this.sessionName = "sessionName"
            this.durationSeconds = 600
        }.build()

        val creds = provider.getCredentials()
    }

}