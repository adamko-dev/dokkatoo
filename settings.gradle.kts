rootProject.name = "dokkatoo"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

  repositories {
    mavenCentral()
  }
}

includeBuild("./build-operations/build-logic")
includeBuild("./build-operations/release-management")
includeBuild("./dokkatoo-components")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
