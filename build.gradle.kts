import buildsrc.utils.excludeGeneratedGradleDsl
import buildsrc.utils.initIdeProjectLogo

plugins {
  buildsrc.conventions.base

  idea
}

group = "dev.adamko.dokkatoo"
version = "1.0.1-SNAPSHOT"


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

initIdeProjectLogo()
