package buildsrc.utils

import org.gradle.api.Project

/** Sets a logo for project IDEs */
fun Project.initIdeProjectLogo() {
  val logoSvg = rootProject.layout.projectDirectory.file("media/img/logo.svg")
  val ideaDir = rootProject.layout.projectDirectory.dir(".idea")

  if (
    logoSvg.asFile.exists()
    && ideaDir.asFile.exists()
    && !ideaDir.file("icon.png").asFile.exists()
    && !ideaDir.file("icon.svg").asFile.exists()
  ) {
    copy {
      from(logoSvg) { rename { "icon.svg" } }
      into(ideaDir)
    }
  }
}
