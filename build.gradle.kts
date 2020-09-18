/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform") version "1.4.10"
}

group = "software.amazon.awssdk.crt"
version = "1.0-SNAPSHOT"
description = "Kotlin Multiplatform bindings for AWS SDK Common Runtime"

repositories {
    mavenLocal()
    mavenCentral()
    maven ("https://dl.bintray.com/kotlin/kotlin-eap")
    maven ("https://kotlin.bintray.com/kotlinx")
}

project.ext.set("hostManager", HostManager())
apply(from = rootProject.file("gradle/utility.gradle"))
apply(from = rootProject.file("gradle/native.gradle"))


kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }

        val jvmTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }

        // see native.gradle for how native sourceSets are configured

    }

    // TODO - setup cmake task

    sourceSets.all {
        println("configuring source set $name")
        val srcDir = if (name.endsWith("Main")) "src" else "test"
        val resourcesPrefix = if (name.endsWith("Test")) "test-" else ""
        // source set name should always be the platform followed by a suffix of either "Main" or "Test
        // e.g. jvmMain, commonTest, etc
        val platform = name.substring(0, name.length - 4)

        kotlin.srcDir("src/$platform/$srcDir")
        resources.srcDir("src/$platform/${resourcesPrefix}resources")
    }


    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        val target = this
        compilations["main"].cinterops {
            val interopDir = "$projectDir/src/native/interop"
            val awsLibs = listOf(
                "aws-c-common"
            )

            awsLibs.forEach { name ->
                println("configuring cinterop for: $name [${target.name}]")
                create(name){
                    // strip off `aws-c-`
                    val suffix = name.substring(6)
                    val headerDir = "$rootDir/aws-common-runtime/$name/include"
                    println("header dir: $headerDir")
                    defFile("$interopDir/$suffix.def")
                    includeDirs(headerDir)
                }
            }
        }
    }
}
