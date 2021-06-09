package aws.sdk.kotlin.crt.io

import java.nio.ByteBuffer

/**
 * Represents a mutable linear range of bytes that can be written to.
 * Instance of this class has no additional state except the bytes themselves.
 *
 * NOTE: Platform implementations should provide direct access to the underlying bytes
 */
public actual class MutableBuffer(public val buffer: ByteBuffer) {

    /**
     * The amount of remaining write capacity before the buffer is full
     */
    public actual val writeRemaining: Int get() = buffer.remaining()

    /**
     * Write as much of [length] bytes from [src] as possible starting at [offset].
     * The number of bytes written is returned which may be less than [length]
     */
    public actual fun write(src: ByteArray, offset: Int, length: Int): Int {
        val wc = minOf(writeRemaining, length)
        buffer.put(src, offset, wc)
        return wc
    }

    public actual companion object {
        public actual fun of(src: ByteArray): MutableBuffer = MutableBuffer(ByteBuffer.wrap(src))
    }
}
