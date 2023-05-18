package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE

/**
 * Produces a Dokka Configuration that describes a single module of a multimodule Dokka configuration.
 *
 * @see dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
 */
@CacheableTask
abstract class DokkatooPrepareModuleDescriptorTask
@DokkatooInternalApi
@Inject
constructor() : DokkatooTask() {

  @get:OutputFile
  abstract val dokkaModuleDescriptorJson: RegularFileProperty

  @get:Input
  abstract val moduleName: Property<String>

  @get:Input
  abstract val modulePath: Property<String>

  @get:InputDirectory
  @get:PathSensitive(RELATIVE)
  abstract val moduleDirectory: DirectoryProperty

  @get:InputFiles
  @get:Optional
  @get:PathSensitive(RELATIVE)
  abstract val includes: ConfigurableFileCollection

  @TaskAction
  internal fun generateModuleConfiguration() {
    val moduleName = moduleName.get()
    val moduleDirectory = moduleDirectory.asFile.get()
    val includes = includes.files
    val modulePath = modulePath.get()

    val moduleDesc = DokkaModuleDescriptionKxs(
      name = moduleName,
      sourceOutputDirectory = moduleDirectory,
      includes = includes,
      modulePath = modulePath,
    )

    val encodedModuleDesc = jsonMapper.encodeToString(moduleDesc)

    logger.info("encodedModuleDesc: $encodedModuleDesc")

    dokkaModuleDescriptorJson.get().asFile.writeText(encodedModuleDesc)
  }
}
