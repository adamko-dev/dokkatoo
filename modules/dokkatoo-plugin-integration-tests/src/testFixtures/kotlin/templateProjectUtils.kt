package  dev.adamko.dokkatoo.utils

import java.nio.file.Path
import kotlin.io.path.*

fun GradleProjectTest.copyExampleProject(path: String) {
  copy(
    src = GradleProjectTest.exampleProjectsDir.resolve(path),
    dest = projectDir
  )
}

fun GradleProjectTest.copyIntegrationTestProject(path: String) {
  copy(
    src = GradleProjectTest.integrationTestProjectsDir.resolve(path),
    dest = projectDir
  )
}

private fun GradleProjectTest.copy(src: Path, dest: Path) {
  dest.createDirectories()
  src.copyToRecursively(dest, followLinks = false, overwrite = true)

  dest.walk()
    .filter { it.isRegularFile() }
    .forEach { f ->
      f.updateKgpVersion(version = versions.kgp)
      f.updateAgpVersion(version = versions.agp)

      if (versions.gradle <= "8.0")
        f.writeText(
          f.readText()
            .replace(
              """providers.gradleProperty("testMavenRepo")""",
              """providers.gradleProperty("testMavenRepo").forUseAtConfigurationTime()""",
            )
        )
    }
}

private fun Path.updateKgpVersion(version: Version) {
  writeText(
    readText()
      .replace(
        """implementation\("org\.jetbrains\.kotlin:kotlin-gradle-plugin:[^"]+"\)""".toRegex(),
        """implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$version")""",
      )
      .replace("""kotlin\("(?<type>[^"]+)"\) version "?[^"]+"?""".toRegex()) { mr ->
        val type = mr.groups["type"]!!.value
        """kotlin("$type") version "$version""""
      }
  )
}

private fun Path.updateAgpVersion(version: Version) {
  writeText(
    readText()
      .replace("""id\("com.android(?<type>[^"]*)"\) version "[^"]+"""".toRegex()) { mr ->
        val type = mr.groups["type"]!!.value
        """id("com.android$type") version "$version""""
      }
  )
}
