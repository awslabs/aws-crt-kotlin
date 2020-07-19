plugins {
    kotlin("multiplatform") version "1.3.72"
}

group = "software.amazon.awssdk.crt"
version = "1.0-SNAPSHOT"
description = "Kotlin Multiplatform bindings for AWS SDK Common Runtime"

repositories {
    mavenLocal()
    mavenCentral()
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
           dependsOn(nativeMain)
        }
        configure(nativeTargets.map{getByName("${it}Test")}) {
            dependsOn(nativeTest)
        }

    }

    // TODO - setup c-interops
    // TODO - setup cmake task

    sourceSets.all {
        println("configuring source set $name")
        val srcDir = if (name.endsWith("Main")) "src" else "test"
        val resourcesPrefix = if (name.endsWith("Test")) "test-" else ""
        // source set name should always be the platform followed by a suffix of either "Main" or "Test
        // e.g. jvmMain, commonTest, etc
        val platform = name.substring(0, name.length - 4)
        kotlin.srcDir("src/$platform/$srcDir")
        resources.srcDir("src/$platform/${resourcesPrefix}resources")
    }
}
