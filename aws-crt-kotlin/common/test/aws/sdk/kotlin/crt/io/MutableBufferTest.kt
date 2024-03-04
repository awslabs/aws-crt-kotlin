import aws.sdk.kotlin.crt.CrtTest
import aws.sdk.kotlin.crt.io.MutableBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class MutableBufferTest : CrtTest() {
    @Test
    fun testWriteToFullBuffer() {
        val str = "Hello!"
        val bytes = str.encodeToByteArray()
        val buffer = MutableBuffer.of(bytes) // creates a full buffer

        assertEquals(0, buffer.writeRemaining)

        // since it's full, should write 0 bytes
        assertEquals(0, buffer.write(bytes))
    }
}