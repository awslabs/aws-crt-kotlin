/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.awssdk.kotlin.crt

public actual object Log {

    /**
     * Logs a message at the specified level
     * @param level level attached to the log record (for filtering purposes)
     * @param subject the subject of the log record (for filtering purposes)
     * @param message string to write
     */
    public actual fun log(
        level: LogLevel,
        subject: LogSubject,
        message: String
    ) {
        TODO("not implemented")
    }

    public actual fun error(subject: LogSubject, message: String): Unit =
        log(LogLevel.Error, subject, message)

    public actual fun warn(subject: LogSubject, message: String): Unit =
        log(LogLevel.Warn, subject, message)

    public actual fun info(subject: LogSubject, message: String): Unit =
        log(LogLevel.Info, subject, message)

    public actual fun debug(subject: LogSubject, message: String): Unit =
        log(LogLevel.Debug, subject, message)

    public actual fun trace(subject: LogSubject, message: String): Unit =
        log(LogLevel.Trace, subject, message)
}
