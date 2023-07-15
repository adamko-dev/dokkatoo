import dokkatoo.utils.jetBrains

rootProject.name = "dokkatoo-components"

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
    google()
    jetBrains {
      intellijRepositorySnapshots()
      intellijRepositoryReleases()
      kotlinIde()
      kotlinIdePluginDependencies()
      intellijDependencies()
      rdSnapshots()
    }
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}

include(
  ":docs",
  ":dokkatoo-plugin",
)
