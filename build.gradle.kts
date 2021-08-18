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

val ideaActive = System.getProperty("idea.active") == "true"
extra["ideaActive"] = ideaActive


kotlin {
    explicitApi()

    jvm {
        attributes {
            attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment.STANDARD_JVM)
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
    if (!ideaActive) {

        // NOTE: We don't actually need the Android plugin. All of the Android specifics are handled in aws-crt-java,
        // we just need a variant with a different dependency set + some distinguishing attributes.
        jvm("android") {
            attributes {
                attribute(
                    org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.Companion.attribute,
                    org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm
                )
                attribute(
                    TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                    objects.named(TargetJvmEnvironment.ANDROID)
                )
            }
        }
    }

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


        if (!ideaActive) {
            val androidMain by getting {
                // re-use the jvm (desktop) sourceSet. We only really care about declaring a variant with a different set
                // of runtime dependencies
                kotlin.srcDir("src/jvm/src")
                dependsOn(commonMain)
                dependencies {
                    val crtJavaVersion: String by project
                    // we need symbols we can resolve during compilation but at runtime (i.e. on device) we depend on the Android dependency
                    compileOnly("software.amazon.awssdk.crt:aws-crt:$crtJavaVersion")
                    implementation("software.amazon.awssdk.crt:aws-crt-android:$crtJavaVersion@aar")

                    // FIXME - temporary integration with CompletableFuture while we work out a POC on the jvm target
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
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
