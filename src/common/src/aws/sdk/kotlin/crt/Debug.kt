/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.crt

private const val ENV_CRT_DEBUG = "CRTDEBUG"

/**
 * The CRTDEBUG environment variable controls debugging within the common runtime. It is a comma separated
 * list of name=val pairs setting these named variables:
 *
 * trace: setting trace=(0,1,2) sets the memory trace level for allocations
 *
 */
internal object CrtDebug {
    val traceLevel: Int

    init {
        // parse env variable
        val debug = Platform.getenv(ENV_CRT_DEBUG) ?: ""
        val fields = debug.split(",").associate { assignment ->
            val field = assignment.split("=", limit = 2)
            if (field.size >= 2) field[0] to field[1] else field[0] to ""
        }

        traceLevel = fields["trace"]?.toIntOrNull() ?: 0
    }
}
