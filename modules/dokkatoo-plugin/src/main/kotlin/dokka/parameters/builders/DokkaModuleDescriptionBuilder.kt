package dev.adamko.dokkatoo.dokka.parameters.builders

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionSpec
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.File
import java.io.IOException
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.DokkaSourceSetImpl

/**
 * Convert the Gradle-focused [DokkaModuleDescriptionSpec] into a [DokkaSourceSetImpl] instance,
 * which will be passed to Dokka Generator.
 *
 * The conversion is defined in a separate class to try and prevent classes from Dokka Generator
 * leaking into the public API.
 */
// TODO create
@DokkatooInternalApi
internal object DokkaModuleDescriptionBuilder {

  fun build(
    spec: DokkaModuleDescriptionSpec,
//    includes: Set<File>,
//    sourceOutputDirectory: File,
  ): DokkaModuleDescriptionImpl {
    val moduleDescriptorJson = spec.moduleDescriptorJson.asFile.get()

    val moduleKxs = try {
      val fileContent = moduleDescriptorJson.readText()
      jsonMapper.decodeFromString(
        DokkaModuleDescriptionKxs.serializer(),
        fileContent,
      )
    } catch (ex: Exception) {
      throw IOException("Could not parse DokkaModuleDescriptionKxs from $moduleDescriptorJson", ex)
    }

    return DokkaModuleDescriptionImpl(
      name = moduleKxs.name,
      relativePathToOutputDirectory = File(
        moduleKxs.modulePath.removePrefix(":").replace(':', '/')
      ),
      includes = spec.includes.files,
      sourceOutputDirectory = spec.sourceOutputDirectory.asFile.get(),
    )
  }
}
