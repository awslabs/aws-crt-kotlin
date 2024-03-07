/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

internal fun headerPair(raw: String): Pair<String, String> {
    val parts = raw.split(":", limit = 2)
    require(parts.size == 2) { "invalid HTTP header specified: $raw " }
    return parts[0] to parts[1]
}

fun createFileSink(path: String): RawSink = SystemFileSystem.sink(Path(path))

class StdoutSink : RawSink {

    override fun write(source: Buffer, byteCount: Long) {
        val data = source.readByteArray()
        println(data.decodeToString())
    }
    override fun flush() {}
    override fun close() { }
}
