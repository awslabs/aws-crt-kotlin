/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    application
}

kotlin {
    // applyDefaultHierarchyTemplate()
    // macosX64()
    // macosArm64()
    // linuxX64()
    // FIXME - not supported by kotlinx-cli
    // linuxArm64()

    jvm {
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
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.cli)
                implementation(project(":aws-crt-kotlin"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.io)
            }
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries {
            executable("elasticurl", listOf(DEBUG)) {
                // entryPoint = "software.amazon.awssdk.kotlin.crt.elasticurl.ApplicationKt"
            }
        }
    }
}

application {
    mainClass = "ApplicationKt"
}
