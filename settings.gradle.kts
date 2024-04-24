/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "kotlinRepoTools"
            url = java.net.URI("https://d2gys1nrxnjnyg.cloudfront.net/releases")
        }
    }
}

rootProject.name = "aws-crt-kotlin-parent"

include(":aws-crt-kotlin")
include(":elasticurl")
