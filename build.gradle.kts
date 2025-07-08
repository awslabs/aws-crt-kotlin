/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configureLinting
import aws.sdk.kotlin.gradle.dsl.configureNexus
import aws.sdk.kotlin.gradle.util.typedProp
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    alias(libs.plugins.kotlinx.binary.compatibility.validator)
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.aws.kotlin.repo.tools.kmp)
    alias(libs.plugins.aws.kotlin.repo.tools.artifactsizemetrics)
}

artifactSizeMetrics {
    artifactPrefixes = setOf(":aws-crt-kotlin")
    significantChangeThresholdPercentage = 5.0
    projectRepositoryName = "aws-crt-kotlin"
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    // Enables running `./gradlew allDeps` to get a comprehensive list of dependencies for every subproject
    tasks.register<DependencyReportTask>("allDeps") { }
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.add("-Xjdk-release=1.8")
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
}

if (project.typedProp<Boolean>("kotlinWarningsAsErrors") == true) {
    allprojects {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            compilerOptions.allWarningsAsErrors = true
        }
    }
}

// Publishing
configureNexus()

// Code Style
val lintPaths = listOf(
    "**/*.{kt,kts}",
)

configureLinting(lintPaths)
