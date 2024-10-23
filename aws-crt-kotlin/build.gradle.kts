/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.crt.CMakeBuildType
import aws.sdk.kotlin.gradle.crt.cmakeInstallDir
import aws.sdk.kotlin.gradle.crt.configureCrtCMakeBuild
import aws.sdk.kotlin.gradle.crt.disableCrossCompileTargets
import aws.sdk.kotlin.gradle.dsl.configurePublishing
import aws.sdk.kotlin.gradle.kmp.IDEA_ACTIVE
import aws.sdk.kotlin.gradle.kmp.configureKmpTargets
import aws.sdk.kotlin.gradle.util.typedProp
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.aws.kotlin.repo.tools.kmp)
    id("crt-build-support")
}

val sdkVersion: String by project
group = properties["publishGroupName"] ?: error("missing publishGroupName")
version = sdkVersion
description = "Kotlin Multiplatform bindings for AWS Common Runtime"

// See: https://kotlinlang.org/docs/reference/opt-in-requirements.html#opting-in-to-using-api
val optinAnnotations = listOf("kotlin.RequiresOptIn", "kotlinx.cinterop.ExperimentalForeignApi")

// KMP configuration from build plugin
configureKmpTargets()

kotlin {
    explicitApi()

    // FIXME - move to repo-tools plugin
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosArm64()
    iosX64()
    linuxX64()
    linuxArm64()
    // FIXME - setup docker files and cmake tasks appropriately
    // mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
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

    // create a single "umbrella" cinterop will all the aws-c-* API's we want to consume
    // see: https://github.com/JetBrains/kotlin-native/issues/2423#issuecomment-466300153
    targets.withType<KotlinNativeTarget> {
        val knTarget = this
        logger.info("configuring $knTarget: ${knTarget.name}")
        val cmakeInstallTask = configureCrtCMakeBuild(knTarget, CMakeBuildType.Release)
        val targetInstallDir = project.cmakeInstallDir(knTarget)
        val headerDir = targetInstallDir.resolve("include")
        val libDir = targetInstallDir.resolve("lib")

        compilations["main"].cinterops {
            val interopDir = "$projectDir/native/interop"
            println("configuring crt cinterop for: ${knTarget.name}")
            val interopSettings = create("aws-crt") {
                defFile("$interopDir/crt.def")
                includeDirs(headerDir)
                compilerOpts("-L${libDir.absolutePath}")
                extraOpts("-libraryPath", libDir.absolutePath)
            }

            // cinterop tasks processes header files which requires the corresponding CMake build/install to run
            val cinteropTask = tasks.named(interopSettings.interopProcessingTaskName)
            cinteropTask.configure {
                dependsOn(cmakeInstallTask)
            }
        }
    }
}

// disable "standalone" mode in simulator tests since it causes TLS issues. this means we need to manage the simulator
// ourselves (booting / shutting down). FIXME: https://youtrack.jetbrains.com/issue/KT-38317
kotlin {
    val simulatorDeviceName = project.findProperty("iosSimulatorDevice") as? String ?: "iPhone 15"

    val xcrun = "/usr/bin/xcrun"

    tasks.register<Exec>("bootIosSimulatorDevice") {
        isIgnoreExitValue = true
        commandLine(xcrun, "simctl", "boot", simulatorDeviceName)

        doLast {
            val result = executionResult.get()
            val code = result.exitValue
            if (code != 148 && code != 149) { // ignore "simulator already running" errors
                result.assertNormalExitValue()
            }
        }
    }

    tasks.register<Exec>("shutdownIosSimulatorDevice") {
        mustRunAfter(tasks.withType<KotlinNativeSimulatorTest>())
        commandLine(xcrun, "simctl", "shutdown", simulatorDeviceName)

        doLast {
            executionResult.get().assertNormalExitValue()
        }
    }

    tasks.withType<KotlinNativeSimulatorTest>().configureEach {
        if (!HostManager.hostIsMac) {
            return@configureEach
        }

        dependsOn("bootIosSimulatorDevice")
        finalizedBy("shutdownIosSimulatorDevice")

        standalone = false
        device = simulatorDeviceName
    }
}

// Publishing
configurePublishing("aws-crt-kotlin")

val linuxTargets: List<String> = listOf(
    "linuxX64",
    "linuxArm64",
)

// create a summary task that compiles all cross platform test binaries
tasks.register("linuxTestBinaries") {
    linuxTargets.map {
        tasks.named("${it}TestBinaries")
    }.forEach { testTask ->
        dependsOn(testTask)
    }
}

val disableCrossCompile = typedProp<Boolean>("aws.sdk.kotlin.crt.disableCrossCompile") == true
if (disableCrossCompile) {
    logger.warn("aws.sdk.kotlin.crt.disableCrossCompile=true: Cross compilation is disabled.")
    disableCrossCompileTargets()
}

// run tests on specific JVM version
val testJavaVersion = typedProp<String>("test.java.version")?.let {
    JavaLanguageVersion.of(it)
}?.also {
    println("configuring tests to run with jdk $it")
}

if (testJavaVersion != null) {
    tasks.withType<Test> {
        val toolchains = project.extensions.getByType<JavaToolchainService>()
        javaLauncher.set(
            toolchains.launcherFor {
                languageVersion.set(testJavaVersion)
            },
        )
    }
}
