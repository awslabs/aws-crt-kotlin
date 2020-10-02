/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

fun main(args: Array<String>) {
    println("hello world")

    val opts = CliOpts.from(args)

    println("url: ${opts.url}")
}
