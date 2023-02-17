import buildsrc.utils.excludeGeneratedGradleDsl

plugins {
  buildsrc.conventions.base

  idea

  //id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.12.1"
}

group = "dev.adamko.dokkatoo"
version = "0.0.2-SNAPSHOT"


idea {
  module {
    isDownloadSources = true
    isDownloadJavadoc = false
    excludeGeneratedGradleDsl(layout)
    excludeDirs = excludeDirs + layout.files(
      ".idea",
      "gradle/kotlin-js-store",
      "gradle/wrapper",
      "externals/kotlin-dokka",
    )
  }
}
