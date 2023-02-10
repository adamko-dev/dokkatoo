rootProject.name = "dokkatoo"

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

@Suppress("UnstableApiUsage") // Central declaration of repositories is an incubating feature
dependencyResolutionManagement {

  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

  repositories {
    mavenCentral()
    google()

    maven("https://www.jetbrains.com/intellij-repository/snapshots")
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://www.myget.org/F/rd-snapshots/maven/")

    ivy("https://github.com/") {
      name = "GitHub Release"
      patternLayout {
        artifact("[organization]/[module]/archive/[revision].[ext]")
        artifact("[organization]/[module]/archive/refs/tags/[revision].[ext]")
        artifact("[organization]/[module]/archive/refs/tags/v[revision].[ext]")
      }
      metadataSources { artifact() }
    }
  }
}

include(
  ":modules:dokkatoo-plugin",

  ":externals",
)


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
