import buildsrc.utils.excludeGeneratedGradleDsl

plugins {
  buildsrc.conventions.base

  idea
}

group = "dev.adamko"
version = "0.0.1-SNAPSHOT"


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
