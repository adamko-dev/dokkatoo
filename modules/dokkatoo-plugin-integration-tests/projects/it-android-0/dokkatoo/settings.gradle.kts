rootProject.name = "it-android-0"

pluginManagement {
  repositories {
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
    mavenCentral()
    google()
    gradlePluginPortal()
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
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
    mavenCentral()
    google()
  }
}
