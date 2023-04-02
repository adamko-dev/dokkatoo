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

    excludeDirs.apply {
      addAll(
        layout.files(
          ".idea",
          "gradle/kotlin-js-store",
          "gradle/wrapper",
          "externals/kotlin-dokka",
        )
      )
      // exclude .gradle dirs from nested projects (e.g. example/template projects)
      addAll(
        layout.projectDirectory.asFile.walk()
          .filter { it.isDirectory && it.name == ".gradle" }
          .flatMap { file ->
            file.walk().maxDepth(1).filter { it.isDirectory }.toList()
          }
      )
    }
  }
}

initIdeProjectLogo("media/img/logo.svg")
