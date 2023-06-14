import buildsrc.utils.excludeGeneratedGradleDsl
import buildsrc.utils.initIdeProjectLogo

plugins {
  buildsrc.conventions.base
  idea
}

group = "dev.adamko.dokkatoo"
version = "1.6.0-SNAPSHOT"


idea {
  module {
    excludeGeneratedGradleDsl(layout)

    excludeDirs.apply {
      // exclude .gradle, IDE dirs from nested projects (e.g. example & template projects)
      // so IntelliJ project-wide search isn't cluttered with irrelevant files
      val excludedDirs = setOf(
        ".idea",
        ".gradle",
        "build",
        "gradle/wrapper",
        "ANDROID_SDK",
      )
      addAll(
        projectDir.walk().filter { file ->
          excludedDirs.any {
            file.invariantSeparatorsPath.endsWith(it)
          }
        }
      )
    }
  }
}

initIdeProjectLogo("modules/docs/images/logo-icon.svg")
