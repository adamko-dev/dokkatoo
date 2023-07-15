import dokkatoo.utils.gitHubRelease

rootProject.name = "dokkatoo-integration-tests"

pluginManagement {
  includeBuild("../build-tools/build-plugins")
  includeBuild("../build-tools/settings-plugins")
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("dokkatoo.conventions.settings-base")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

  repositories {
    mavenCentral()
    gradlePluginPortal()
    gitHubRelease()
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}

includeBuild("../dokkatoo-examples/")
