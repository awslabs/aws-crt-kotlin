/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
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
