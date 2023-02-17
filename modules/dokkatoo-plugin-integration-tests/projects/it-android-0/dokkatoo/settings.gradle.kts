rootProject.name = "it-android-0"

pluginManagement {
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
