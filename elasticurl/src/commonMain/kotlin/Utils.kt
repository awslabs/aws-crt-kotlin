/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal fun headerPair(raw: String): Pair<String, String> {
    val parts = raw.split(":", limit = 2)
    require(parts.size == 2) { "invalid HTTP header specified: $raw " }
    return parts[0] to parts[1]
}

/**
 * MPP compatible runBlocking to run suspend functions from common
 */
internal expect fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T

/**
 * Platform specific initialization steps
 */
internal expect fun platformInit()

/**
 * Output sink
 */
interface Sink {
    fun write(data: ByteArray)
    fun close()
}

internal expect fun createFileSink(filename: String): Sink

/**
 * Sink that just echoes the data to stdout
 */
internal class StdoutSink : Sink {
    override fun write(data: ByteArray) {
        println(data.decodeToString())
    }
    override fun close() {}
}
