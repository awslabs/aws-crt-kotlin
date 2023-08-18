/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    kotlin("multiplatform")
    application
}

kotlin {
    jvm {
        withJava()
        val jvmJar by tasks.getting(org.gradle.jvm.tasks.Jar::class) {
            doFirst {
                manifest {
                    attributes["Main-Class"] = "ApplicationKt"
                }
                from(configurations.getByName("runtimeClasspath").map { if (it.isDirectory) it else zipTree(it) })
            }

            duplicatesStrategy = DuplicatesStrategy.WARN
        }
        attributes {
            attribute(org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.Companion.attribute, org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm)
            attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                val kotlinxCliVersion: String by project
                val coroutinesVersion: String by project

                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlinxCliVersion")
                implementation(project(":aws-crt-kotlin"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }
    }
}

application {
    mainClassName = "ApplicationKt"
}
