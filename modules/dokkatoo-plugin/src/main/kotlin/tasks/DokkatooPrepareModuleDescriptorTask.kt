package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Produces a Dokka Configuration that describes a single module of a multimodule Dokka configuration.
 *
 * @see dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
 */
//@CacheableTask
@Deprecated("not build-cache compatible")
abstract class DokkatooPrepareModuleDescriptorTask
@DokkatooInternalApi
@Inject
constructor() : DokkatooTask() {

  @get:OutputFile
  abstract val moduleDescriptor: RegularFileProperty

  @get:Input
  abstract val moduleName: Property<String>

  @get:Input
  abstract val modulePath: Property<String>

//  @get:InputDirectory
//  @get:PathSensitive(RELATIVE)
//  abstract val moduleDirectory: DirectoryProperty
//
//  @get:InputFiles
//  @get:Optional
//  @get:PathSensitive(RELATIVE)
//  abstract val includes: ConfigurableFileCollection

  @TaskAction
  internal fun generateModuleConfiguration() {
    val moduleName = moduleName.get()
    val modulePath = modulePath.get()

    val moduleDesc = DokkaModuleDescriptionKxs(
      name = moduleName,
      modulePath = modulePath,
    )

    val encodedModuleDesc =
      jsonMapper.encodeToString(DokkaModuleDescriptionKxs.serializer(), moduleDesc)

    logger.info("encodedModuleDesc: $encodedModuleDesc")

    moduleDescriptor.get().asFile.writeText(encodedModuleDesc)
  }

  //region deprecated properties
  @Deprecated("Renamed to moduleDescriptorJson", ReplaceWith("moduleDescriptor"))
  @get:ReplacedBy("moduleDescriptor")
  @Suppress("unused")
  val dokkaModuleDescriptorJson: RegularFileProperty by ::moduleDescriptor
  //endregion
}
