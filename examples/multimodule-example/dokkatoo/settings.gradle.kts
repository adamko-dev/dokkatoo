rootProject.name = "dokkatoo-multimodule-example"

pluginManagement {
    plugins {
        kotlin("jvm") version "1.7.20"
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(file("../../../../test-maven-repo"))
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven(file("../../../../test-maven-repo"))
    }
}

include(":parentProject")
include(":parentProject:childProjectA")
include(":parentProject:childProjectB")
