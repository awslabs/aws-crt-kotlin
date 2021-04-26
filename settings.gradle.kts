pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "aws-crt-kotlin"
enableFeaturePreview("GRADLE_METADATA")

/*
FIXME: we used kotlinx-cli in the elasticurl example and it is now breaking due to bintray shutting down
disabling because it's breaking downstream consumers of crt-kotlin, re-enable after it's published somewhere else
or rewrite without the dependency.

see: https://github.com/Kotlin/kotlinx-cli/issues/23
*/
//include(":elasticurl")

// for local development against crt-java checkout into same parent directory and use composite builds
// if (file("../aws-crt-java").exists()) {
//     println("including aws-crt-java as composite build")
//     includeBuild("../aws-crt-java")
// }