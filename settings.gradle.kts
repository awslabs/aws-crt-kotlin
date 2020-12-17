pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven ("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

rootProject.name = "aws-crt-kotlin"
enableFeaturePreview("GRADLE_METADATA")

include(":elasticurl")

// for local development against crt-java checkout into same parent directory and use composite builds
// if (file("../aws-crt-java").exists()) {
//     println("including aws-crt-java as composite build")
//     includeBuild("../aws-crt-java")
// }