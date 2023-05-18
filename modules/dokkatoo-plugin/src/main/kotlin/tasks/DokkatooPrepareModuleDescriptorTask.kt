package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

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

  @get:Input
  abstract val moduleName: Property<String>

  @get:Input
  abstract val modulePath: Property<String>

  @get:OutputFile
  abstract val dokkaModuleDescriptorJson: RegularFileProperty

  @TaskAction
  internal fun generateModuleConfiguration() {
    val moduleDesc = DokkaModuleDescriptionKxs(
      name = moduleName.get(),
      modulePath = modulePath.get(),
    )

    val encodedModuleDesc = jsonMapper.encodeToString(moduleDesc)

    logger.info("encodedModuleDesc: $encodedModuleDesc")

    dokkaModuleDescriptorJson.get().asFile.writeText(encodedModuleDesc)
  }
}
