/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.crt.io

///**
// * Represents a mutable linear range of bytes that can be written to.
// * Instance of this class has no additional state except the bytes themselves.
// *
// * NOTE: Platform implementations should provide direct access to the underlying bytes
// */
//public actual class MutableBuffer {
//    /**
//     * The amount of remaining write capacity before the buffer is full
//     */
//    public actual val writeRemaining: Int
//        get() = TODO("Not yet implemented")
//
//    /**
//     * Write as much of [length] bytes from [src] as possible starting at [offset].
//     * The number of bytes written is returned which may be less than [length]
//     */
//    public actual fun write(src: ByteArray, offset: Int, length: Int): Int {
//        TODO("Not yet implemented")
//    }
//
//    public actual companion object {
//        /**
//         * Create a buffer instance backed by [src]
//         */
//        public actual fun of(src: ByteArray): MutableBuffer {
//            TODO("Not yet implemented")
//        }
//    }
//}
