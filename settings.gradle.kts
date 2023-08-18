/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    // configure default plugin versions
    plugins {
        val kotlinVersion: String by settings
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("org.jetbrains.kotlin.multiplatform") version kotlinVersion
    }
}

sourceControl {
    gitRepository(java.net.URI("https://github.com/awslabs/aws-kotlin-repo-tools.git")) {
        producesModule("aws.sdk.kotlin:build-plugins")
        producesModule("aws.sdk.kotlin:ktlint-rules")
    }
}

rootProject.name = "aws-crt-kotlin-parent"

include(":aws-crt-kotlin")
include(":elasticurl")
