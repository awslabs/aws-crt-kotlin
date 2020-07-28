plugins {
    kotlin("multiplatform") version "1.4-M3"
}

group = "software.amazon.awssdk.crt"
version = "1.0-SNAPSHOT"
description = "Kotlin Multiplatform bindings for AWS SDK Common Runtime"

repositories {
    mavenLocal()
    mavenCentral()
    maven ("https://dl.bintray.com/kotlin/kotlin-eap")
    maven ("https://kotlin.bintray.com/kotlinx")
}


kotlin {
    jvm()

    linuxX64()
    val nativeTargets = listOf("linuxX64")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
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
            }
        }

        val jvmTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val nativeTest by creating {
            dependsOn(commonTest)
        }

        // shared main/test source sets for native targets
        configure(nativeTargets.map{getByName("${it}Main")}) {
            println("configuring: $name to depend on nativeMain")
            dependsOn(nativeMain)
        }
        configure(nativeTargets.map{getByName("${it}Test")}) {
            dependsOn(nativeTest)
        }

    }

    // TODO - setup cmake task

    sourceSets.all {
        println("configuring source set $name")
        val srcDir = if (name.endsWith("Main")) "src" else "test"
        val resourcesPrefix = if (name.endsWith("Test")) "test-" else ""
        // source set name should always be the platform followed by a suffix of either "Main" or "Test
        // e.g. jvmMain, commonTest, etc
        val platform = name.substring(0, name.length - 4)

//        if (nativeTargets.contains(platform)) {
//            /* FIXME - hack for shared source set (native) not resolving packages created by cinterops.
//             * C interop dependencies import just fine in concrete target source sets (e.g. linuxX64/src)
//             * but not in shared source set native targets depend on.
//             * This makes native targets (e.g. linuxX64) src dir the same as the native source set to
//             * make the IDE happy. We need a better workaround though.
//             * see: https://youtrack.jetbrains.com/issue/KT-36086
//             */
//            kotlin.srcDir("src/native/$srcDir")
//            resources.srcDir("src/native/${resourcesPrefix}resources")
//        } else {
//            kotlin.srcDir("src/$platform/$srcDir")
//            resources.srcDir("src/$platform/${resourcesPrefix}resources")
//        }
            kotlin.srcDir("src/$platform/$srcDir")
            resources.srcDir("src/$platform/${resourcesPrefix}resources")
    }


    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].cinterops {
            val interopDir = "$projectDir/src/native/interop"
            val awsLibs = listOf(
                "aws-c-common"
            )

            awsLibs.forEach { name ->
                println("configuring cinterop for: $name")
                create(name){
                    // strip off `aws-c-`
                    val suffix = name.substring(6)
                    val headerDir = "$rootDir/aws-common-runtime/$name/include"
                    println("header dir: $headerDir")
                    defFile("$interopDir/$suffix.def")
                    includeDirs(headerDir)
                }
            }
        }
    }
}
