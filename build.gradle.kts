/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configureLinting
import aws.sdk.kotlin.gradle.dsl.configureNexus
import aws.sdk.kotlin.gradle.util.typedProp
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        // Add our custom gradle plugin(s) to buildscript classpath (comes from github source)
        // NOTE: buildscript classpath for the root project is the parent classloader for the subprojects, we
        // only need to include it here, imports in subprojects will work automagically
        classpath("aws.sdk.kotlin:build-plugins") {
            version {
                require("0.2.5")
            }
        }
    }
}

plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

if (project.typedProp<Boolean>("kotlinWarningsAsErrors") == true) {
    allprojects {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.allWarningsAsErrors = true
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

apiValidation {
    ignoredProjects += setOf("elasticurl")
}
