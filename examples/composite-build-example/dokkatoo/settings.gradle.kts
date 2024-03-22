rootProject.name = "dokkatoo-composite-build-example"

pluginManagement {
  includeBuild("build-logic")
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
  repositories {
    mavenCentral()
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

includeBuild("docs")
includeBuild("module-kea")
includeBuild("module-kakapo")
