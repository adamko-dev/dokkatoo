rootProject.name = "library-publishing-example-dokkatoo"

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
