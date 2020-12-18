/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

plugins {
    kotlin("multiplatform")
    application
}

project.ext.set("hostManager", org.jetbrains.kotlin.konan.target.HostManager())
apply(from = rootProject.file("gradle/utility.gradle"))
// apply(from = rootProject.file("gradle/native.gradle"))


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

    sourceSets {
        commonMain {
            dependencies {
                val kotlinxCliVersion: String by project
                val coroutinesVersion: String by project

                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlinxCliVersion")
                implementation(project(":"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion-native-mt")
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
            "aws-c-cal",
            "aws-c-io",
            "aws-c-http",
            "aws-c-compression"
        )
        val linkDirs = awsLibs.map {
            val rootBuildDir = rootProject.buildDir
            "-L$rootBuildDir/cmake-build/crt/$it"
        }.toMutableList()

        if (rootProject.ext.has("extraLinkDirs")) {
            val extraLinkDirs = rootProject.ext.get("extraLinkDirs") as List<String>
            println("[elasticurl] extraLinkDirs: $extraLinkDirs")
            linkDirs.addAll(extraLinkDirs)
        }

        val linkOpts = linkDirs.joinToString(" ")

        println("[elasticurl] linker opts: $linkOpts")
        compilations["main"].kotlinOptions {
            freeCompilerArgs = listOf("-linker-options", linkOpts)
        }
    }

}

application {
    mainClassName = "ApplicationKt"
}
