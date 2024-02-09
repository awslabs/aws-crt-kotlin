package aws.sdk.kotlin.gradle.crt

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.listProperty
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/*
cmakeConfigure<NativeTargetName>   -- e.g. cmakeConfigureLinuxX64
cmakeBuild<NativeTargetName>       -- e.g. cmakeBuildLinuxX64
cmakeInstall<NativeTargetName>     -- e.g. cmakeInstallLinuxX64

CMake tasks may or may not run inside of a docker container depending on the native target being built.

All linux targets are built in a container.

*/

enum class CMakeBuildType {
    Debug,
    RelWithDebInfo,
    Release
}

fun Project.configureCrtCMakeBuild(
    knTarget: KotlinNativeTarget,
    buildType: CMakeBuildType = CMakeBuildType.RelWithDebInfo
) {
    val cmakeConfigure = registerCmakeConfigureTask(knTarget, buildType)

    val cmakeBuild = registerCmakeBuildTask(knTarget, buildType)
    cmakeBuild.configure {
        dependsOn(cmakeConfigure)
    }

    val cmakeInstall = registerCmakeInstallTask(knTarget, buildType)
    cmakeInstall.configure {
        dependsOn(cmakeBuild)
    }

}

internal fun Project.registerCmakeConfigureTask(
    knTarget: KotlinNativeTarget,
    buildType: CMakeBuildType
): TaskProvider<Task> {
    val cmakeBuildDir = project.cmakeBuildDir(knTarget)
    val installDir = project.cmakeInstallDir(knTarget)

    val relativeBuildDir = cmakeBuildDir.relativeTo(project.rootDir).path
    val relativeInstallDir = installDir.relativeTo(project.rootDir).path
    val cmakeLists = project.rootProject.projectDir.resolve("CMakeLists.txt")

    return project.tasks.register(knTarget.namedSuffix("cmakeConfigure", capitalized = true)) {
        group = "ffi"

        inputs.property("buildType", buildType.toString())
        inputs.file(cmakeLists)
        outputs.file(cmakeBuildDir.resolve("CMakeCache.txt"))

        doLast {
            val args = mutableListOf(
                "-B$relativeBuildDir",
                // "-H$relCmakeLists",
                "-DCMAKE_BUILD_TYPE=$buildType",
                "-DCMAKE_INSTALL_PREFIX=$relativeInstallDir",
                "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
                "-DBUILD_DEPS=ON",
                "-DBUILD_TESTING=OFF"
            )

            if (HostManager.hostIsMac && knTarget.konanTarget.family.isAppleFamily) {
                args.add("-GXcode")

                // see https://cmake.org/cmake/help/latest/manual/cmake-toolchains.7.html#cross-compiling-for-ios-tvos-visionos-or-watchos
                val osxArch = when (knTarget.konanTarget.architecture) {
                    Architecture.X64 -> "x86_64"
                    Architecture.X86 -> "i386"
                    Architecture.ARM64 -> "arm64"
                    Architecture.ARM32 -> when(knTarget.konanTarget) {
                        KonanTarget.WATCHOS_ARM32 -> "armv7k"
                        KonanTarget.WATCHOS_ARM64 -> "arm64_32"
                        else -> null
                    }
                    else -> null
                }
                osxArch?.let {
                    args.add("-DCMAKE_OSX_ARCHITECTURES=$it")
                }

                val osxSystemName = when(knTarget.konanTarget.family) {
                    Family.IOS -> "iOS"
                    Family.TVOS -> "tvOS"
                    Family.WATCHOS -> "watchOS"
                    else -> null
                }
                osxSystemName?.let {
                    args.add("-DCMAKE_SYSTEM_NAME=$it")
                }
            }

            // executed from root build dir which is where CMakeLists.txt is
            // We _could_ use the undocumented -H flag but that will be harder to make work inside docker
            args.add(".")

            runCmake(project, knTarget, args)
        }
    }

}

// See https://cmake.org/cmake/help/latest/manual/cmake-toolchains.7.html#cross-compiling-for-ios-tvos-visionos-or-watchos
const val IOS_DEVICE_SDK = "iphoneos"
const val IOS_SIMULATOR_SDK = "iphonesimulator"
const val TVOS_DEVICE_SDK = "appletvos"
const val TVOS_SIMULATOR_SDK = "appletvsimulator"
const val WATCHOS_DEVICE_SDK = "watchos"
const val WATCHOS_SIMULATOR_SDK = "watchsimulator"

internal fun Project.registerCmakeBuildTask(
    knTarget: KotlinNativeTarget,
    buildType: CMakeBuildType
): TaskProvider<Task> {
    val cmakeBuildDir = project.cmakeBuildDir(knTarget)
    val relativeBuildDir = cmakeBuildDir.relativeTo(project.rootDir).path

    return project.tasks.register(knTarget.namedSuffix("cmakeBuild", capitalized = true)) {
        group = "ffi"

        inputs.property("buildType", buildType.toString())
        inputs.file(project.cmakeLists)
        inputs.files(fileTree("$rootDir/crt").matching {
            include(listOf("**/CMakeLists.txt", "**/*.c", "**/*.h"))
        })

        doLast {

            val args = mutableListOf(
                "--build",
                relativeBuildDir,
                "--config",
                buildType.toString()
            )

            val osxSdk = when(knTarget.konanTarget) {
                KonanTarget.IOS_ARM64 -> IOS_DEVICE_SDK
                KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.IOS_X64 -> IOS_SIMULATOR_SDK
                KonanTarget.TVOS_ARM64 -> TVOS_DEVICE_SDK
                KonanTarget.TVOS_SIMULATOR_ARM64, KonanTarget.TVOS_X64 -> TVOS_SIMULATOR_SDK
                KonanTarget.WATCHOS_ARM32,
                KonanTarget.WATCHOS_ARM64,
                KonanTarget.WATCHOS_DEVICE_ARM64 -> WATCHOS_DEVICE_SDK
                KonanTarget.WATCHOS_SIMULATOR_ARM64,
                KonanTarget.WATCHOS_X64,
                KonanTarget.WATCHOS_X86 -> WATCHOS_SIMULATOR_SDK
                else -> null
            }

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


internal fun Project.registerCmakeInstallTask(
    knTarget: KotlinNativeTarget,
    buildType: CMakeBuildType
): TaskProvider<Task> {
    val cmakeBuildDir = project.cmakeBuildDir(knTarget)
    val relativeBuildDir = cmakeBuildDir.relativeTo(project.rootDir).path

    return project.tasks.register(knTarget.namedSuffix("cmakeInstall", capitalized = true)) {
        group = "ffi"

        inputs.file(project.cmakeLists)

        doLast {
            val args = mutableListOf(
                "--install",
                relativeBuildDir,
                "--config",
                buildType.toString()
            )
            runCmake(project, knTarget, args)
        }
    }

}

fun KotlinNativeTarget.namedSuffix(prefix: String, capitalized: Boolean = false): String =
    prefix + if (capitalized) name.capitalized() else name


fun Project.cmakeBuildDir(target: KotlinNativeTarget): File =
    project.rootProject.layout.buildDirectory.file(target.namedSuffix("cmake-build/")).get().asFile

fun Project.cmakeInstallDir(target: KotlinNativeTarget): File =
    project.rootProject.layout.buildDirectory.file(target.namedSuffix("crt-libs/")).get().asFile

val Project.cmakeLists: File
    get() = rootProject.projectDir.resolve("CMakeLists.txt")

internal fun runCmake(project: Project, target: KotlinNativeTarget, cmakeArgs: List<String>) {
    project.exec {
        workingDir(project.rootDir)
        val exeArgs = cmakeArgs.toMutableList()
        val exeName = when(target.konanTarget) {
            KonanTarget.LINUX_X64, KonanTarget.LINUX_ARM64 -> {
                // cross compiling via dockcross - set the docker exe to cmake
                exeArgs.add(0, "cmake")
                "./dockcross-" + target.konanTarget.name.replace("_", "-")
            }
            else -> "cmake"
        }

        project.logger.info("$exeName ${exeArgs.joinToString(separator=" ")}")
        executable(exeName)
        args(exeArgs)
    }
}