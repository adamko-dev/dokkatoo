rootProject.name = "kotlin-as-java-example"

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
