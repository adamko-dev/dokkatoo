package buildsrc.utils

import java.io.File
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.plugins.ide.idea.model.IdeaModel


/**
 * Exclude directories containing
 *
 * - generated Gradle code,
 * - IDE files,
 * - Gradle config,
 *
 * so they don't clog up search results.
 */
fun Project.excludeProjectConfigurationDirs(
  idea: IdeaModel
) {
  val excludedDirs = providers.of(IdeaExcludedDirectoriesSource::class) {
    parameters.projectDir.set(layout.projectDirectory)
  }.get()

  idea.module.excludeDirs.addAll(excludedDirs)
}

// Have to use a ValueSource to find the files, otherwise Gradle
// considers _all files_ an input for configuration cache 🙄
internal abstract class IdeaExcludedDirectoriesSource :
  ValueSource<Set<File>, IdeaExcludedDirectoriesSource.Parameters> {

  interface Parameters : ValueSourceParameters {
    val projectDir: DirectoryProperty
  }

  override fun obtain(): Set<File> {
    val projectDir = parameters.projectDir.get().asFile

    val doNotWalkDirs = setOf(
      ".git",
      ".kotlin",
    )

    val generatedSrcDirs = listOf(
      "kotlin-dsl-accessors",
      "kotlin-dsl-external-plugin-spec-builders",
      "kotlin-dsl-plugins",
    )

    val generatedDirs = projectDir
      .walk()
      .onEnter { it.name !in doNotWalkDirs && it.parentFile.name !in generatedSrcDirs }
      .filter { it.isDirectory }
      .filter { it.parentFile.name in generatedSrcDirs }
      .flatMap { file ->
        file.walk().maxDepth(1).filter { it.isDirectory }.toList()
      }
      .toSet()

    // exclude .gradle, IDE dirs from nested projects (e.g. example & template projects)
    // so IntelliJ project-wide search isn't cluttered with irrelevant files
    val projectDirsToExclude = setOf(
      ".idea",
      ".gradle",
      "build",
      "gradle/wrapper",
      "ANDROID_SDK",
      "examples/versioning-multimodule-example/dokkatoo/previousDocVersions",
      "examples/versioning-multimodule-example/dokka/previousDocVersions",
      "modules/dokkatoo-plugin-integration-tests/example-project-data",
    )

    val excludedProjectDirs = projectDir
      .walk()
      .onEnter { it.name !in doNotWalkDirs }
//      .filter { it.isDirectory }
      .filter { dir ->
        projectDirsToExclude.any {
          dir.invariantSeparatorsPath.endsWith("/$it")
        }
      }
      .toSet()

    // can't use buildSet {} https://github.com/gradle/gradle/issues/28325
    return mutableSetOf<File>().apply {
      addAll(generatedDirs)
      addAll(excludedProjectDirs)
    }
  }
}


/**
 * Sets a logo for project IDEs.
 *
 * (Avoid updating the logo during project configuration,
 * instead piggyback off a random task that runs on IJ import.)
 */
fun Task.initIdeProjectLogo(
  svgLogoPath: String
) {
  val fs = project.serviceOf<FileSystemOperations>()

  val logoSvg = project.layout.projectDirectory.file(svgLogoPath)
  val ideaDir = project.layout.projectDirectory.dir(".idea")
  // don't register task inputs, we don't really care about up-to-date checks

  doLast("initIdeProjectLogo") {
    if (
      logoSvg.asFile.exists()
      && ideaDir.asFile.exists()
      && !ideaDir.file("icon.png").asFile.exists()
      && !ideaDir.file("icon.svg").asFile.exists()
    ) {
      fs.copy {
        from(logoSvg) { rename { "icon.svg" } }
        into(ideaDir)
      }
    }
  }
}
