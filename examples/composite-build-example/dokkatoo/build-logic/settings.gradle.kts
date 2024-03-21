rootProject.name = "build-logic"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    exclusiveContent {
      forRepository {
        maven(providers.gradleProperty("testMavenRepo")) {
          name = "DokkatooTestMavenRepo"
        }
      }
      filter {
        includeGroup("dev.adamko.dokkatoo")
        includeGroup("dev.adamko.dokkatoo-html")
        includeGroup("dev.adamko.dokkatoo-javadoc")
        includeGroup("dev.adamko.dokkatoo-jekyll")
        includeGroup("dev.adamko.dokkatoo-gfm")
      }
    }
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {

  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

  repositories {
    mavenCentral()
    gradlePluginPortal()
    exclusiveContent {
      forRepository {
        maven(providers.gradleProperty("testMavenRepo")) {
          name = "DokkatooTestMavenRepo"
        }
      }
      filter {
        includeGroup("dev.adamko.dokkatoo")
        includeGroup("dev.adamko.dokkatoo-html")
        includeGroup("dev.adamko.dokkatoo-javadoc")
        includeGroup("dev.adamko.dokkatoo-jekyll")
        includeGroup("dev.adamko.dokkatoo-gfm")
      }
    }
  }
}