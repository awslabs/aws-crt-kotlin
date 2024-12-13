/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configurePublishing
import aws.sdk.kotlin.gradle.kmp.IDEA_ACTIVE
import aws.sdk.kotlin.gradle.kmp.configureKmpTargets

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

val sdkVersion: String by project
group = properties["publishGroupName"] ?: error("missing publishGroupName")
version = sdkVersion
description = "Kotlin Multiplatform bindings for AWS SDK Common Runtime"

// See: https://kotlinlang.org/docs/reference/opt-in-requirements.html#opting-in-to-using-api
val optinAnnotations = listOf("kotlin.RequiresOptIn")

// KMP configuration from build plugin
configureKmpTargets()

kotlin {
    explicitApi()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.crt.java)

                // FIXME - temporary integration with CompletableFuture while we work out a POC on the jvm target
                implementation(libs.kotlinx.coroutines.jdk8)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.debug)
                implementation(libs.mockserver.netty)
            }
        }
    }

    sourceSets.all {
        optinAnnotations.forEach { languageSettings.optIn(it) }
    }
}

// Publishing
configurePublishing("aws-crt-kotlin")
