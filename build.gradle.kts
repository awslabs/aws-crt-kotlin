/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.util.Properties

plugins {
    kotlin("multiplatform")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

val sdkVersion: String by project
group = "aws.sdk.kotlin.crt"
version = sdkVersion
description = "Kotlin Multiplatform bindings for AWS SDK Common Runtime"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

// See: https://kotlinlang.org/docs/reference/opt-in-requirements.html#opting-in-to-using-api
val experimentalAnnotations = listOf("kotlin.RequiresOptIn")


kotlin {
    explicitApi()

    jvm()

    val kotlinVersion: String by project
    val coroutinesVersion: String by project

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                // native multithreading support for coroutines is not stable...
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion-native-mt")
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
                val crtJavaVersion: String by project
                implementation("software.amazon.awssdk.crt:aws-crt:$crtJavaVersion")

                // FIXME - temporary integration with CompletableFuture while we work out a POC on the jvm target
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
            }
        }

        val jvmTest by getting {
            dependencies {
                val junitVersion: String by project
                api("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
                api("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
                implementation("org.junit.jupiter:junit-jupiter:$junitVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
            }
        }

    }

    sourceSets.all {
        println("configuring source set $name")
        val srcDir = if (name.endsWith("Main")) "src" else "test"
        val resourcesPrefix = if (name.endsWith("Test")) "test-" else ""
        // source set name should always be the platform followed by a suffix of either "Main" or "Test
        // e.g. jvmMain, commonTest, etc
        val platform = name.substring(0, name.length - 4)

        kotlin.srcDir("src/$platform/$srcDir")
        resources.srcDir("src/$platform/${resourcesPrefix}resources")
        experimentalAnnotations.forEach { languageSettings.useExperimentalAnnotation(it) }
    }
}

// have to configure JVM test task to use junit platform when using junit5
val jvmTest: Test by tasks
jvmTest.apply {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }

    useJUnitPlatform()
}

if (project.hasProperty("sonatypeUsername") && project.hasProperty("sonatypePassword")) {
    apply(plugin = "io.github.gradle-nexus.publish-plugin")

    nexusPublishing {
        repositories {
            create("awsNexus") {
                nexusUrl.set(uri("https://aws.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://aws.oss.sonatype.org/content/repositories/snapshots/"))
                username.set(project.property("sonatypeUsername") as String)
                password.set(project.property("sonatypePassword") as String)
            }
        }
    }
}

apply(from = rootProject.file("gradle/publish.gradle"))

val ktlint by configurations.creating
val ktlintVersion: String by project

dependencies {
    ktlint("com.pinterest:ktlint:$ktlintVersion")
}

val lintPaths = listOf(
    "src/**/*.kt",
    "elasticurl/**/*.kt"
)

tasks.register<JavaExec>("ktlint") {
    description = "Check Kotlin code style."
    group = "Verification"
    classpath = configurations.getByName("ktlint")
    main = "com.pinterest.ktlint.Main"
    args = lintPaths
}

tasks.register<JavaExec>("ktlintFormat") {
    description = "Auto fix Kotlin code style violations"
    group = "formatting"
    classpath = configurations.getByName("ktlint")
    main = "com.pinterest.ktlint.Main"
    args = listOf("-F") + lintPaths
}

tasks.check.get().dependsOn(":ktlint")
