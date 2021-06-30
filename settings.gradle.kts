pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    // configure default plugin versions
    plugins {
        val kotlinVersion: String by settings
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("org.jetbrains.kotlin.multiplatform") version kotlinVersion
    }
}

rootProject.name = "aws-crt-kotlin"

include(":elasticurl")

