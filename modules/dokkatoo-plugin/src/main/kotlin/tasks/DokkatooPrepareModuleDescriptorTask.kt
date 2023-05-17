package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
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
constructor(
  objects: ObjectFactory
) : DokkatooTask.WithSourceSets(objects) {

  @get:Input
  abstract val moduleName: Property<String>

  @get:InputDirectory
  @get:PathSensitive(RELATIVE)
  abstract val moduleDirectory: DirectoryProperty

  @get:InputFiles
  @get:Optional
  @get:PathSensitive(RELATIVE)
  abstract val includes: ConfigurableFileCollection

  @get:OutputFile
  abstract val dokkaModuleDescriptorJson: RegularFileProperty

  @get:Input
  abstract val modulePath: Property<String>

  @TaskAction
  internal fun generateModuleConfiguration() {
//    val moduleName = moduleName.get()
//    val moduleDirectory = moduleDirectory.asFile.get()
//    val includes = includes.files
//    val modulePath = modulePath.get()

    val moduleDesc = DokkaModuleDescriptionKxs(
      name = moduleName.get(),
      modulePath = modulePath.get(),
//      sourceOutputDirectory = moduleDirectory,
//      includes = includes,
    )

    val encodedModuleDesc = jsonMapper.encodeToString(moduleDesc)

    logger.info("encodedModuleDesc: $encodedModuleDesc")

    dokkaModuleDescriptorJson.get().asFile.writeText(encodedModuleDesc)
  }
}
