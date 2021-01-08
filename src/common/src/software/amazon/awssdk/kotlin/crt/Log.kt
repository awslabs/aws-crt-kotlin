/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

/**
 * Controls the verbosity of logging records output
 */
public enum class LogLevel(public val value: Int) {
    None(0),
    Fatal(1),
    Error(2),
    Warn(3),
    Info(4),
    Debug(5),
    Trace(6);
}

/**
 * The destination to log to
 */
public enum class LogDestination {
    None,
    Stdout,
    Stderr,
    File;
}
