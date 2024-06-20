package  dev.adamko.dokkatoo.utils

import java.nio.file.Path
import kotlin.io.path.*

fun GradleProjectTest.copyExampleProject(path: String) {
  val src = GradleProjectTest.exampleProjectsDir.resolve(path)
  copy(src = src, dest = projectDir)
}

fun GradleProjectTest.copyIntegrationTestProject(path: String) {
  val src = GradleProjectTest.integrationTestProjectsDir.resolve(path)
  copy(src = src, dest = projectDir)
}

private fun GradleProjectTest.copy(src: Path, dest: Path) {
  src.copyToRecursively(dest, followLinks = false, overwrite = true)
  dest.updateKgpVersion(version = versions.kgp)
  dest.updateAgpVersion(version = versions.agp)
}

private fun Path.updateKgpVersion(
  version: String
) {
  walk().filter { it.isRegularFile() }
    .forEach { f ->
      f.writeText(
        f.readText()
          .replace(
            """implementation\("org\.jetbrains\.kotlin:kotlin-gradle-plugin:[^"]+"\)""".toRegex(),
            """implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$version")""",
          )
          .replace("""kotlin\("(?<type>[^"]+)"\) version "[^"]+"""".toRegex()) { mr ->
            val type = mr.groups["type"]!!.value
            """kotlin("$type") version "$version""""
          }
      )
    }
}

private fun Path.updateAgpVersion(
  version: String
) {
  walk().filter { it.isRegularFile() }
    .forEach { f ->
      f.writeText(
        f.readText()
          .replace("""id\("com.android(?<type>[^"]*)"\) version "[^"]+"""".toRegex()) { mr ->
            val type = mr.groups["type"]!!.value
            """id("com.android$type") version "$version""""
          }
      )
    }
}
