rootProject.name = "gradle-example-dokkatoo"

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
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
