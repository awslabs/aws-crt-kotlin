/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

plugins {
    kotlin("multiplatform")
    application
}


kotlin {
    jvm() {
        withJava()
        val jvmJar by tasks.getting(org.gradle.jvm.tasks.Jar::class) {
            doFirst {
                manifest {
                    attributes["Main-Class"] = "ApplicationKt"
                }
                from(configurations.getByName("runtimeClasspath").map { if (it.isDirectory) it else zipTree(it) })
            }
        }
    }

    // TODO - when intellij is running we can only build for the variant we have configured - re-use the common native stuffs
    // linuxX64()
    macosX64()
    // mingwX64()

    sourceSets {
        commonMain {
            dependencies {
                val kotlinxCliVersion: String by project
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlinxCliVersion")
                implementation(project(":"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries {
            executable("elasticurl", listOf(DEBUG)) {
                // entryPoint = "software.amazon.awssdk.kotlin.crt.elasticurl.ApplicationKt"
            }
        }

        val awsLibs = listOf(
            "aws-c-common",
            "aws-c-io",
            "aws-c-http",
            "aws-c-compression"
        )
        val linkDirs = awsLibs.map {
            val rootBuildDir = rootProject.buildDir
            "-L$rootBuildDir/cmake-build/aws-common-runtime/$it"
        }
        val linkOpts = linkDirs.joinToString(" ")

        compilations["main"].kotlinOptions {
            freeCompilerArgs = listOf("-linker-options", linkOpts)
        }
    }

}

application {
    mainClassName = "ApplicationKt"
}
