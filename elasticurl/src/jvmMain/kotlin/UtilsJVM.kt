/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import kotlinx.coroutines.CoroutineScope
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.runBlocking as kotlinRunBlocking

internal actual fun <T> runBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    kotlinRunBlocking { block() }

internal actual fun platformInit() { }

internal actual fun createFileSink(filename: String): Sink = FileSinkJvm(filename)

private class FileSinkJvm(filename: String) : Sink {
    val writer = FileOutputStream(filename)

    override fun write(data: ByteArray) {
        writer.write(data)
    }
    override fun close() {
        writer.close()
    }
}
