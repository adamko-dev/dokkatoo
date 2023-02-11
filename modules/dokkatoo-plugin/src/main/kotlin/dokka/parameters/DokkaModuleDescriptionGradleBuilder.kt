package dev.adamko.dokkatoo.dokka.parameters

import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.DokkaConfigurationBuilder

abstract class DokkaModuleDescriptionGradleBuilder @Inject constructor(
  @get:Input
  val moduleName: String,
) : DokkaConfigurationBuilder<DokkaParametersKxs.DokkaModuleDescriptionKxs>, Named {

  @get:Input
  abstract val sourceOutputDirectory: RegularFileProperty

  @get:Input
  abstract val includes: ConfigurableFileCollection

  @Internal
  override fun build() =
    DokkaParametersKxs.DokkaModuleDescriptionKxs(
      moduleName = moduleName,
      sourceOutputDirectory = sourceOutputDirectory.get().asFile,
      includes = includes.files,
    )
}