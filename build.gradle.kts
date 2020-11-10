/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset
import java.util.Properties

plugins {
    kotlin("multiplatform") version "1.4.10"
}

group = "software.amazon.awssdk.crt"
version = "1.0-SNAPSHOT"
description = "Kotlin Multiplatform bindings for AWS SDK Common Runtime"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven ("https://dl.bintray.com/kotlin/kotlin-eap")
        maven ("https://kotlin.bintray.com/kotlinx")
    }
}

project.ext.set("hostManager", HostManager())
apply(from = rootProject.file("gradle/utility.gradle"))
apply(from = rootProject.file("gradle/native.gradle"))

// See: https://kotlinlang.org/docs/reference/opt-in-requirements.html#opting-in-to-using-api
val experimentalAnnotations = listOf("kotlin.RequiresOptIn")

val coroutinesVersion: String by project

fun isLinux(target: KotlinNativeTarget): Boolean = when(target.name) {
    "linuxX64" -> true
    "native" -> {
        // using intellij and only building on current host
        // the actual preset has the real target name. See utility.gradle
        val ideaPresetName = project.ext.get("ideaPresetName") as String
        ideaPresetName == "linuxX64"
    }
    else -> false
}

// get a project propety by name if it exists (including from local.properties)
inline fun<reified T> getProperty(name: String): T? {
    if (project.hasProperty(name)) {
        return project.properties.get(name) as T
    }

    val localProperties = Properties()
    val propertiesFile: File = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { localProperties.load(it) }

        if (localProperties.containsKey(name)) {
            return localProperties.get(name) as T
        }
    }

    return null
}

kotlin {
    explicitApi()

    jvm()

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
                // FIXME - hack for local development against branch version of aws-crt-java for the moment
                val crtJavaHome: String = getProperty("crtJavaHome") ?: throw GradleException("need to set `crtJavaHome` using either `-PcrtJavaHome=PATH` or in local.properties")
                val crtJavaJar = "$crtJavaHome/target/aws-crt-1.0.0-SNAPSHOT.jar"
                println("crt java jar: $crtJavaJar")
                implementation(files(crtJavaJar))

                // FIXME - temporary integration with CompletableFuture while we work out a POC on the jvm target
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
            }
        }

        val jvmTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }

        // see native.gradle for how native sourceSets are configured

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


    // create a single "umbrella" cinterop will all the aws-c-* API's we want to consume
    // see: https://github.com/JetBrains/kotlin-native/issues/2423#issuecomment-466300153
    targets.withType<KotlinNativeTarget> {
        val knTarget = this

        val awsLibs = listOf(
            "aws-c-common",
            "aws-c-io",
            "aws-c-http",
            "aws-c-compression"
        )

        compilations["main"].cinterops {
            val interopDir = "$projectDir/src/native/interop"

            // cmake configured files need included
            val generatedIncludeDirs = listOf(
                "$buildDir/cmake-build/aws-common-runtime/aws-c-common/generated/include"
            )

            println("configuring cinterop for crt: [${knTarget.name}]")
            create("aws-crt"){
                val includeDirs = awsLibs.map { name ->
                    val headerDir = "$rootDir/aws-common-runtime/$name/include"
                    println("header dir: $headerDir")
                    headerDir
                }

                defFile("$interopDir/crt.def")
                includeDirs(includeDirs, generatedIncludeDirs)
            }
        }


        // FIXME - we will likely have to make some plugin to deal with this and make it easy on end users
        // libs are specified in the interop (crt.def) file. We just need the link dirs so they can be found
        val linkDirs = awsLibs.map {
            "-L$buildDir/cmake-build/aws-common-runtime/$it"
        }.toMutableList()

        if (isLinux(knTarget)) {
            // s2n is placed in the "lib" dir
            val extraLinkDirs = mutableListOf("-L$buildDir/cmake-build/lib")

            // find libcrypto
            val libcryptoPath: String = getProperty("libcryptoPath") ?: throw GradleException("need to set `libcryptoPath` using either `-PlibcryptoPath=PATH` or in local.properties")
            println("using libcryptoPath: $libcryptoPath")
            extraLinkDirs.add("-L$libcryptoPath")

            // FIXME - set these so elasticurl project can re-use them and link
            project.ext.set("extraLinkDirs", extraLinkDirs)

            linkDirs.addAll(extraLinkDirs)
        }

        val linkOpts = linkDirs.joinToString(" ")
        println("linker opts: $linkOpts")

        compilations["test"].kotlinOptions {
            freeCompilerArgs = listOf("-linker-options", linkOpts)
        }
    }
}

var buildType = "Debug"
if (project.hasProperty("buildType")) {
    buildType = project.property("buildType").toString()
    logger.info("Using custom build type: $buildType")
}

val cmakeConfigure = tasks.register("cmakeConfigure") {
    var cmakeArgs = listOf(
        "-B${buildDir}/cmake-build",
        "-H${rootDir}",
        "-DCMAKE_BUILD_TYPE=${buildType}",
        "-DCMAKE_INSTALL_PREFIX=${buildDir}/cmake-build",
        "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
        "-DBUILD_DEPS=ON",
        "-DBUILD_TESTING=OFF"
    )

//    if (targetOs.startsWith("linux")) {
//        libcryptoPath = "/opt/openssl"
//        // To set this, add -PlibcryptoPath=/path/to/openssl/home on the command line
//        if (project.hasProperty("libcryptoPath")) {
//            libcryptoPath = project.property("libcryptoPath").toString()
//            logger.info("Using project libcrypto path: ${libcryptoPath}")
//        }
//    }
//
//    if (libcryptoPath != null) {
//        cmakeArgs += listOf(
//            "-DLibCrypto_INCLUDE_DIR=${libcryptoPath}/include",
//            "-DLibCrypto_STATIC_LIBRARY=${libcryptoPath}/lib/libcrypto.a"
//        )
//    }

    inputs.property("buildType", buildType)
    inputs.file("${rootDir}/CMakeLists.txt")
    outputs.file("${buildDir}/cmake-build/CMakeCache.txt")

    doLast {
        val argsStr = cmakeArgs.joinToString(separator=" ")
        logger.info("cmake $argsStr")
        exec {
            executable("cmake")
            args(cmakeArgs)
        }
    }
}

// FIXME - we probably need a cmake configure/build per Kotlin/Native target we are setup to build
val cmakeBuild = tasks.register("cmakeBuild") {
    dependsOn(cmakeConfigure)
    inputs.property("buildType", buildType)
    inputs.file("${rootDir}/CMakeLists.txt")
    inputs.file("${buildDir}/cmake-build/CMakeCache.txt")
//    inputs.files(fileTree("${rootDir}/src/native").matching {
//        include(listOf("**/*.c", "**/*.h"))
//    })
    inputs.files(fileTree("${rootDir}/aws-common-runtime").matching {
        include(listOf("**/CMakeLists.txt", "**/*.c", "**/*.h"))
    })
//    outputs.files(fileTree("${buildDir}/cmake-build/lib/${targetOs}/${targetArch}"))

    var cmakeArgs = listOf(
        "--build", "${buildDir}/cmake-build",
        "--config", buildType,
        "--target", "all"
    )

    doLast {
        val argsStr = cmakeArgs.joinToString(separator=" ")
        logger.info("cmake ${argsStr}")
        exec {
            executable("cmake")
            args(cmakeArgs)
        }
    }
}

// cinterop requires all headers to be available which unfortunately requires
// cmake generated headers (e.g. aws-c-common/config.h)
tasks.filter { it.name.startsWith("cinterop") }.forEach {
    it.dependsOn(cmakeConfigure)
}

// running native tests requires linking
val nativeTestTasks = listOf("nativeTest", "linuxX64Test", "macosX64Test", "mingwX64Test")
nativeTestTasks.forEach {
    tasks.findByName(it)?.dependsOn(cmakeBuild)
}

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
