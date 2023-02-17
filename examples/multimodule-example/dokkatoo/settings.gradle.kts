rootProject.name = "dokkatoo-multimodule-example"

pluginManagement {
  plugins {
    kotlin("jvm") version "1.7.20"
  }
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven(providers.gradleProperty("testMavenRepo").map(::file))
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven(providers.gradleProperty("testMavenRepo").map(::file))
  }
}

include(":parentProject")
include(":parentProject:childProjectA")
include(":parentProject:childProjectB")
