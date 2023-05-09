package dev.adamko.dokkatoo.dokka.parameters.builders

import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionSpec
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.File
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
    includes: Set<File>,
    sourceOutputDirectory: File,
  ): DokkaModuleDescriptionImpl =
    DokkaModuleDescriptionImpl(
      name = spec.name,
      relativePathToOutputDirectory = File(
        spec.projectPath.get().removePrefix(":").replace(':', '/')
      ),
      includes = includes,
      sourceOutputDirectory = sourceOutputDirectory,
    )
}
