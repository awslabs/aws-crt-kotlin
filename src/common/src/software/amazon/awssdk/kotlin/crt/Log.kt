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

public enum class LogSubject(public val value: Int) {
    // aws-c-common
    CommonGeneral(0x000),
    CommonTaskScheduler(0x001),

    // aws-c-io
    IoGeneral(0x400),
    IoEventLoop(0x401),
    IoSocket(0x402),
    IoSocketHandler(0x403),
    IoTls(0x404),
    IoAlpn(0x405),
    IoDns(0x406),
    IoPki(0x407),
    IoChannel(0x408),
    IoChannelBootstrap(0x409),
    IoFileUtils(0x40A),
    IoSharedLibrary(0x40B),

    // aws-c-http
    HttpGeneral(0x800),
    HttpConnection(0x801),
    HttpServer(0x802),
    HttpStream(0x803),
    HttpConnectionManager(0x804),
    HttpWebsocket(0x805),
    HttpWebsocketSetup(0x806),

    // aws-c-mqtt
    MqttGeneral(0x1400),
    MqttClient(0x1401),
    MqttTopicTree(0x1402),

    // aws-c-auth
    AuthGeneral(0x1800),
    AuthProfile(0x1801),
    AuthCredentialsProvider(0x1802),
    AuthSigning(0x1803),

    // aws-crt-kotlin, we're authoritative
}

public expect object Log {
    /**
     * Logs a message at the specified level
     * @param level level attached to the log record (for filtering purposes)
     * @param subject the subject of the log record (for filtering purposes)
     * @param message string to write
     */
    public fun log(level: LogLevel, subject: LogSubject, message: String): Unit

    public fun error(subject: LogSubject, message: String): Unit
    public fun warn(subject: LogSubject, message: String): Unit
    public fun info(subject: LogSubject, message: String): Unit
    public fun debug(subject: LogSubject, message: String): Unit
    public fun trace(subject: LogSubject, message: String): Unit
}
