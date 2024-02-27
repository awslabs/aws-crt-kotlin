/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.crt.cmakeInstallDir
import aws.sdk.kotlin.gradle.crt.configureCrtCMakeBuild
import aws.sdk.kotlin.gradle.crt.disableCrossCompileTargets
import aws.sdk.kotlin.gradle.dsl.configurePublishing
import aws.sdk.kotlin.gradle.kmp.IDEA_ACTIVE
import aws.sdk.kotlin.gradle.kmp.configureKmpTargets
import aws.sdk.kotlin.gradle.util.typedProp
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.aws.kotlin.repo.tools.kmp)
    id("crt-build-support")
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

    jvm {
        attributes {
            attribute<org.gradle.api.attributes.java.TargetJvmEnvironment>(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment.STANDARD_JVM),
            )
        }
    }

    // KMP doesn't support sharing source sets for multiple JVM targets OR JVM + Android targets.
    // We can manually declare a `jvmCommon` target and wire it up. It will compile fine but Intellij does
    // not support this and the developer experience is abysmal. Kotlin/Native suffers a similar problem and
    // we can use the same solution. Simply, if Intellij is running (i.e. the one invoking this script) then
    // assume we are only building for JVM. Otherwise declare the additional JVM target for Android and
    // set the sourceSet the same for both but with different runtime dependencies.
    // See:
    //     * https://kotlinlang.org/docs/mpp-share-on-platforms.html#share-code-in-libraries
    //     * https://kotlinlang.org/docs/mpp-set-up-targets.html#distinguish-several-targets-for-one-platform
    if (!IDEA_ACTIVE) {

        // NOTE: We don't actually need the Android plugin. All of the Android specifics are handled in aws-crt-java,
        // we just need a variant with a different dependency set + some distinguishing attributes.
        jvm("android") {
            attributes {
                attribute(
                    org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.Companion.attribute,
                    org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm,
                )
                attribute(
                    TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                    objects.named(TargetJvmEnvironment.ANDROID),
                )
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.atomicfu)
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

        if (!IDEA_ACTIVE) {
            val androidMain by getting {
                // re-use the jvm (desktop) sourceSet. We only really care about declaring a variant with a different set
                // of runtime dependencies
                kotlin.srcDir("jvm/src")
                dependsOn(commonMain)
                dependencies {
                    // we need symbols we can resolve during compilation but at runtime (i.e. on device) we depend on the Android dependency
                    compileOnly(libs.crt.java)
                    val crtJavaVersion = libs.versions.crt.java.version.get()
                    implementation("software.amazon.awssdk.crt:aws-crt-android:$crtJavaVersion@aar")

                    // FIXME - temporary integration with CompletableFuture while we work out a POC on the jvm target
                    implementation(libs.kotlinx.coroutines.jdk8)
                }
            }

            // disable compilation of android test source set. It is the same as the jvmTest sourceSet/tests. This
            // sourceSet only exists to create a new variant that is the same in every way except the runtime
            // dependency on aws-crt-android. To test this we would need to run it on device/emulator.
            tasks.getByName("androidTest").enabled = false
            tasks.getByName("compileTestKotlinAndroid").enabled = false
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
        val cmakeInstallTask = configureCrtCMakeBuild(knTarget)
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
            }

            // cinterop tasks processes header files which requires the corresponding CMake build/install to run
            val cinteropTask = tasks.named(interopSettings.interopProcessingTaskName)
            cinteropTask.configure {
                dependsOn(cmakeInstallTask)
            }
        }

        compilations["test"].compilerOptions.configure {
            freeCompilerArgs.addAll(listOf("-linker-options", "-L${libDir.absolutePath}"))
        }
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
