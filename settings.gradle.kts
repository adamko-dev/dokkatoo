rootProject.name = "dokkatoo"

pluginManagement {
  includeBuild("build-tools/build-plugins")
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
    gradlePluginPortal()
  }
}


includeBuild("dokkatoo-components")
includeBuild("dokkatoo-examples")
includeBuild("dokkatoo-integration-tests")
includeBuild("build-tools/release-management")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
