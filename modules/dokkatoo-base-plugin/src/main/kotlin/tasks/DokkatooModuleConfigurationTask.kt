package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooPlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE

/**
 * Produces a Dokka Configuration that describes a single module of a multimodule Dokka configuration.
 */
@CacheableTask
abstract class DokkatooModuleConfigurationTask @Inject constructor(
  private val layout: ProjectLayout,
) : DokkatooTask() {

  @get:Input
  abstract val moduleName: Property<String>

//    @get:Input
//    protected abstract val moduleOutputDirectoryPath: Property<String>
//
//    /**
//     * Evaluated as a file as defined by [org.gradle.api.Project.file].
//     */
//    fun moduleOutputDirectoryPath(path: Any) {
//        moduleOutputDirectoryPath.set(
//            layout.files(path).singleFile.invariantSeparatorsPath
//        )
//    }

  @get:Input
  protected abstract val sourceOutputDirectory: Property<String>

  fun sourceOutputDirectory(path: Any) {
    sourceOutputDirectory.set(
      layout.files(path).singleFile.invariantSeparatorsPath
    )
  }

  @get:InputFiles
  @get:Optional
  @get:PathSensitive(RELATIVE)
  abstract val includes: ConfigurableFileCollection

  @get:OutputFile
  abstract val dokkaModuleConfigurationJson: RegularFileProperty

  @TaskAction
  fun generateModuleConfiguration() {
    val moduleName = moduleName.get()
//        val moduleOutputDirectory = layout.files(moduleOutputDirectoryPath).singleFile
    val sourceOutputDirectory = layout.files(sourceOutputDirectory).singleFile
    val includes = includes.files

    val moduleDesc = DokkaParametersKxs.DokkaModuleDescriptionKxs(
      moduleName = moduleName,
//            moduleOutputDirectory = moduleOutputDirectory,
      sourceOutputDirectory = sourceOutputDirectory,
      includes = includes,
    )

    val encodedModuleDesc = jsonMapper.encodeToString(moduleDesc)

    logger.info("encodedModuleDesc: $encodedModuleDesc")

    dokkaModuleConfigurationJson.get().asFile.writeText(encodedModuleDesc)
  }
}
