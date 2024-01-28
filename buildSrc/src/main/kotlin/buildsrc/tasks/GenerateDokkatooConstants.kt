package buildsrc.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.NONE
import org.tomlj.Toml

@CacheableTask
abstract class GenerateDokkatooConstants @Inject constructor(
  private val fs: FileSystemOperations
) : DefaultTask() {

  @get:OutputDirectory
  abstract val destinationDir: DirectoryProperty

  @get:Input
  abstract val properties: MapProperty<String, String>

  @get:InputDirectory
  @get:PathSensitive(NONE)
  abstract val dokkaSource: DirectoryProperty

  init {
    group = project.name
  }

  @TaskAction
  fun action() {
    val properties = properties.get() + getDokkaDependencyVersions()

    // prepare temp dir
    fs.delete { delete(temporaryDir) }

    // generate file
    val vals = properties.entries
      .sortedBy { it.key }
      .joinToString("\n") { (k, v) ->
        """const val $k = "$v""""
      }.prependIndent("  ")

    temporaryDir.resolve("DokkatooConstants.kt").apply {
      parentFile.mkdirs()
      writeText(
        """
        |package dev.adamko.dokkatoo.internal
        |
        |@DokkatooInternalApi
        |object DokkatooConstants {
        |$vals
        |}
        |
      """.trimMargin()
      )
    }

    // sync file to output dir
    fs.sync {
      from(temporaryDir) {
        into("dev/adamko/dokkatoo/internal/")
      }
      into(destinationDir)
    }
  }

  private fun getDokkaDependencyVersions(): Map<String, String> {
    val dokkaSource = dokkaSource.get().asFile
    val dokkaLibsFile = dokkaSource.resolve("gradle/libs.versions.toml")
    if (!dokkaLibsFile.exists()) {
      error("could not find libs.versions.toml in Dokka source")
    } else {

      val dokkaLibs = Toml.parse(dokkaLibsFile.toPath())
      fun version(name: String): String =
        dokkaLibs.getString("versions.$name")
          ?: error("missing version $name in Dokka's libs.versions.toml")

      return mapOf(
        "DOKKA_DEPENDENCY_VERSION_KOTLINX_HTML" to version("kotlinx-html"),
        "DOKKA_DEPENDENCY_VERSION_KOTLINX_COROUTINES" to version("kotlinx-coroutines"),
        "DOKKA_DEPENDENCY_VERSION_FREEMARKER" to version("freemarker"),
        "DOKKA_DEPENDENCY_VERSION_JETBRAINS_MARKDOWN" to version("jetbrains-markdown"),
      )
    }
  }
}
