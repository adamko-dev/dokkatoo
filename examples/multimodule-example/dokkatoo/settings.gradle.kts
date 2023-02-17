rootProject.name = "dokkatoo-multimodule-example"

pluginManagement {
  plugins {
    kotlin("jvm") version "1.7.20"
  }
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven(providers.gradleProperty("testMavenRepo").map { rootDir.resolve(it) })
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven(providers.gradleProperty("testMavenRepo").map { rootDir.resolve(it) })
  }
}

include(":parentProject")
include(":parentProject:childProjectA")
include(":parentProject:childProjectB")
