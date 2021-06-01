package aws.sdk.kotlin.crt.io

/**
 * Represents a mutable linear range of bytes that can be written to.
 * Instance of this class has no additional state except the bytes themselves.
 *
 * NOTE: Platform implementations should provide direct access to the underlying bytes
 */
public expect class MutableBuffer {

    /**
     * The amount of remaining write capacity before the buffer is full
     */
    public val writeRemaining: Int

    /**
     * Write as much of [length] bytes from [src] as possible starting at [offset].
     * The number of bytes written is returned which may be less than [length]
     */
    public fun write(src: ByteArray, offset: Int = 0, length: Int = src.size - offset): Int
}
