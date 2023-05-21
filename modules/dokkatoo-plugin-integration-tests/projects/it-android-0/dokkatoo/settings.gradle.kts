rootProject.name = "it-android-0"

pluginManagement {
  repositories {
    maven(providers.gradleProperty("testMavenRepo"))
    mavenCentral()
//    mavenLocal()
    google()
    gradlePluginPortal()
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    maven(providers.gradleProperty("testMavenRepo"))
    mavenCentral()
//    mavenLocal()
    google()
  }
}
