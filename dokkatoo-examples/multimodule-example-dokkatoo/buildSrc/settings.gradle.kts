rootProject.name = "buildSrc"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    //maven(providers.gradleProperty("testMavenRepo"))
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {

  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

  repositories {
    mavenCentral()
    gradlePluginPortal()
    //maven(providers.gradleProperty("testMavenRepo"))
  }
}

includeBuild("../../../dokkatoo-components") {
  dependencySubstitution {
    substitute(module("dev.adamko.dokkatoo:dokkatoo-plugin")).using(project(":dokkatoo-plugin"))
  }
}
