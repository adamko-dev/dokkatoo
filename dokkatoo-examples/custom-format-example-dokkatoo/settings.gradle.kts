rootProject.name = "custom-format-example-dokkatoo"

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
