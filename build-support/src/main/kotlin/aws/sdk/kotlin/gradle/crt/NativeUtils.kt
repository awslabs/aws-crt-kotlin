/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.crt

import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * Kotlin/Native Linux and Windows targets are generally enabled on all hosts since
 * the Kotlin toolchain and backend compilers support cross compilation. We
 * are using cinterop and have to compile CRT for those platforms which sometimes
 * requires using docker which isn't always available in CI or setup in users environment.
 *
 * See [KT-30498](https://youtrack.jetbrains.com/issue/KT-30498)
 */
fun Project.disableCrossCompileTargets() {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        configure<KotlinMultiplatformExtension> {
            targets.withType<KotlinNativeTarget> {
                val knTarget = this
                when {
                    HostManager.hostIsMac && (knTarget.isLinux || knTarget.isWindows) -> disable(knTarget)
                    HostManager.hostIsLinux && knTarget.isApple -> disable(knTarget)
                    HostManager.hostIsMingw && (knTarget.isLinux || knTarget.isApple) -> disable(knTarget)
                }
            }
        }
    }
}

private val KotlinNativeTarget.isLinux: Boolean
    get() = konanTarget.family == Family.LINUX

private val KotlinNativeTarget.isApple: Boolean
    get() = konanTarget.family.isAppleFamily

private val KotlinNativeTarget.isWindows: Boolean
    get() = konanTarget.family == Family.MINGW

internal fun Project.disable(knTarget: KotlinNativeTarget) {
    logger.warn("disabling Kotlin/Native target: ${knTarget.name}")
    knTarget.apply {
        binaries.all {
            linkTaskProvider.configure { enabled = false }
        }
        compilations.all {
            cinterops.all {
                tasks.named(interopProcessingTaskName).configure { enabled = false }
            }
            compileTaskProvider.configure { enabled = false }
        }
        mavenPublication {
            tasks.withType<AbstractPublishToMaven>().configureEach {
                onlyIf { publication != this@mavenPublication }
            }
            tasks.withType<GenerateModuleMetadata>().configureEach {
                onlyIf { publication != this@mavenPublication }
            }
        }
    }

    listOf(
        knTarget.cmakeConfigureTaskName,
        knTarget.cmakeBuildTaskName,
        knTarget.cmakeInstallTaskName,
    ).forEach { cmakeTaskName ->
        tasks.named(cmakeTaskName).configure {
            enabled = false
        }
    }
}

// targets that are always cross compiled/ran in docker containers on non-Linux hosts
internal val crossCompileTargets: List<KonanTarget> = listOf(
    KonanTarget.LINUX_X64,
    KonanTarget.LINUX_ARM64,
)
