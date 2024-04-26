/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.crt

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

/**
 * See [CMAKE_BUILD_TYPE](https://cmake.org/cmake/help/latest/variable/CMAKE_BUILD_TYPE.html)
 */
enum class CMakeBuildType {
    Debug,
    RelWithDebInfo,
    Release,
}

/**
 * Configure CMake tasks for building and installing CRT locally for a given Kotlin/Native target.
 *
 * This function sets up the following tasks:
 * * cmakeConfigure<NativeTargetName>   -- e.g. cmakeConfigureLinuxX64
 * * cmakeBuild<NativeTargetName>       -- e.g. cmakeBuildLinuxX64
 * * cmakeInstall<NativeTargetName>     -- e.g. cmakeInstallLinuxX64
 *
 * CMake tasks may or may not run inside of a docker container depending on the native target being built.
 * All linux targets are built in a container.
 *
 * @param knTarget the native target to build CRT for
 * @param buildType the [CMakeBuildType] to build for CMake build type. Defaults to `RelWithDebInfo` since end users
 * can always strip the binary of all debug info.
 * @return the `cmakeInstall` task for the target which can be used to wire up additional task dependency relationships
*/
fun Project.configureCrtCMakeBuild(
    knTarget: KotlinNativeTarget,
    buildType: CMakeBuildType = CMakeBuildType.RelWithDebInfo,
): TaskProvider<Task> {
    val cmakeConfigure = registerCmakeConfigureTask(knTarget, buildType)

    val cmakeBuild = registerCmakeBuildTask(knTarget, buildType)
    cmakeBuild.configure {
        dependsOn(cmakeConfigure)
    }

    val cmakeInstall = registerCmakeInstallTask(knTarget, buildType)
    cmakeInstall.configure {
        dependsOn(cmakeBuild)
    }

    // only enable cmake* tasks if that target is enabled
    val hm = HostManager()
    listOf(cmakeConfigure, cmakeBuild, cmakeInstall).forEach { task ->
        task.configure {
            onlyIf {
                hm.isEnabled(knTarget.konanTarget)
            }
        }
    }

    // TODO - add separate `cleanCMake<KN-Target>` tasks and make the parent `clean` task depend on the individuals
    tasks.named<Delete>("clean") {
        delete(project.rootProject.layout.buildDirectory.dir("cmake-build"))
        delete(project.rootProject.layout.buildDirectory.dir("crt-libs"))
    }

    return cmakeInstall
}

private fun Project.registerCmakeConfigureTask(
    knTarget: KotlinNativeTarget,
    buildType: CMakeBuildType,
): TaskProvider<Task> {
    val cmakeBuildDir = project.cmakeBuildDir(knTarget)
    val installDir = project.cmakeInstallDir(knTarget)

    val relativeBuildDir = cmakeBuildDir.relativeTo(project.rootDir).path
    val relativeInstallDir = installDir.relativeTo(project.rootDir).path
    val cmakeLists = project.rootProject.projectDir.resolve("CMakeLists.txt")

    return project.tasks.register(knTarget.cmakeConfigureTaskName) {
        group = "ffi"

        inputs.property("buildType", buildType.toString())
        inputs.file(cmakeLists)
        outputs.file(cmakeBuildDir.resolve("CMakeCache.txt"))

        doLast {
            val args = mutableListOf(
                "-B$relativeBuildDir",
                "-DCMAKE_BUILD_TYPE=$buildType",
                "-DCMAKE_INSTALL_PREFIX=$relativeInstallDir",
                "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
                "-DBUILD_DEPS=ON",
                "-DBUILD_TESTING=OFF",
            )

            if (HostManager.hostIsMac && knTarget.konanTarget.family.isAppleFamily) {
                args.add("-GXcode")

                // FIXME - What should the min target for ios be? Does it matter for our build? DCMAKE_OSX_DEPLOYMENT_TARGET
                knTarget.konanTarget.osxArchitectureName?.let {
                    args.add("-DCMAKE_OSX_ARCHITECTURES=$it")
                }

                knTarget.konanTarget.osxSystemName?.let {
                    args.add("-DCMAKE_SYSTEM_NAME=$it")
                }

                // Xcode allows switching between device and simulator (via -sdk) even if we only configure one.
                // Unfortunately this breaks during install as there is no way to override and pass `-sdk` for
                // install like there is for `--build`. For simulator devices we set the name explicitly to
                // ensure the correct directory is searched.
                if (knTarget.konanTarget.isSimulatorSdk) {
                    args.add("-DCMAKE_OSX_SYSROOT=${knTarget.konanTarget.osxDeviceSdkName}")
                }
            }

            // executed from root build dir which is where CMakeLists.txt is
            // We _could_ use the undocumented -H flag but that will be harder to make work inside docker
            args.add(".")

            runCmake(project, knTarget, args)
        }
    }
}

private fun Project.registerCmakeBuildTask(
    knTarget: KotlinNativeTarget,
    buildType: CMakeBuildType,
): TaskProvider<Task> {
    val cmakeBuildDir = project.cmakeBuildDir(knTarget)
    val relativeBuildDir = cmakeBuildDir.relativeTo(project.rootDir).path

    return project.tasks.register(knTarget.cmakeBuildTaskName) {
        group = "ffi"

        inputs.property("buildType", buildType.toString())
        inputs.file(project.cmakeLists)
        inputs.files(
            fileTree("$rootDir/crt").matching {
                include(listOf("**/CMakeLists.txt", "**/*.c", "**/*.h"))
            },
        )

        outputs.dir(cmakeBuildDir)

        doLast {
            val args = mutableListOf(
                "--build",
                relativeBuildDir,
                "--config",
                buildType.toString(),
                "--parallel",
                System.getProperty("org.gradle.workers.max", "16"),
            )

            val osxSdk = knTarget.konanTarget.osxDeviceSdkName
            if (osxSdk != null) {
                // see https://cmake.org/cmake/help/latest/manual/cmake-toolchains.7.html#switching-between-device-and-simulator
                // assumes Xcode generator
                args.add("--")
                args.add("-sdk")
                args.add(osxSdk)
            }

            runCmake(project, knTarget, args)
        }
    }
}

private fun Project.registerCmakeInstallTask(
    knTarget: KotlinNativeTarget,
    buildType: CMakeBuildType,
): TaskProvider<Task> {
    val cmakeBuildDir = project.cmakeBuildDir(knTarget)
    val relativeBuildDir = cmakeBuildDir.relativeTo(project.rootDir).path
    val installDir = project.cmakeInstallDir(knTarget)

    return project.tasks.register(knTarget.cmakeInstallTaskName) {
        group = "ffi"

        inputs.file(project.cmakeLists)
        outputs.dir(installDir)

        doLast {
            val args = mutableListOf(
                "--install",
                relativeBuildDir,
                "--config",
                buildType.toString(),
            )
            runCmake(project, knTarget, args)
        }
    }
}

private fun runCmake(project: Project, target: KotlinNativeTarget, cmakeArgs: List<String>) {
    project.exec {
        workingDir(project.rootDir)
        val exeArgs = cmakeArgs.toMutableList()
        val exeName = when {
            target.konanTarget in crossCompileTargets -> {
                // cross compiling via dockcross - set the docker exe to cmake
                val containerScriptArgs = listOf("--args", "--pull=missing", "--", "cmake")
                exeArgs.addAll(0, containerScriptArgs)
                val script = "dockcross-" + target.konanTarget.name.replace("_", "-")
                validateCrossCompileScriptsAvailable(project, script)
                "./$script"
            }
            else -> "cmake"
        }

        project.logger.info("$exeName ${exeArgs.joinToString(separator = " ")}")
        executable(exeName)
        args(exeArgs)
    }
}

private fun validateCrossCompileScriptsAvailable(project: Project, script: String) {
    val scriptFile = project.rootProject.file(script)
    if (!scriptFile.exists()) {
        val message = """
        dockcross script: `$scriptFile` does not exist! Try re-building the relevant docker image(s) and generating
        the cross compile scripts.
        
        e.g. `./docker-images/build-all.sh`
        
        Alternatively disable cross compilation by setting the property `-Paws.sdk.kotlin.crt.disableCrossCompile=true`
        """.trimIndent()
        error(message)
    }
}