rootProject.name = "multimodule-example-dokkatoo"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    //maven(providers.gradleProperty("testMavenRepo"))
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    mavenCentral()
    //maven(providers.gradleProperty("testMavenRepo"))
  }
}

include(":parentProject")
include(":parentProject:childProjectA")
include(":parentProject:childProjectB")
