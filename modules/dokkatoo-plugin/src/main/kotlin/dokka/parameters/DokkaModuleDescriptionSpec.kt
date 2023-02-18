package dev.adamko.dokkatoo.dokka.parameters

import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.DokkaConfigurationBuilder

abstract class DokkaModuleDescriptionSpec @Inject constructor(
  @get:Input
  val moduleName: String,
) : DokkaConfigurationBuilder<DokkaParametersKxs.DokkaModuleDescriptionKxs>, Named {

  @get:Input
  abstract val sourceOutputDirectory: RegularFileProperty

  @get:Input
  abstract val includes: ConfigurableFileCollection

  @get:Input
  abstract val projectPath: Property<String>

  @Internal
  override fun build() =
    DokkaParametersKxs.DokkaModuleDescriptionKxs(
      name = moduleName,
      sourceOutputDirectory = sourceOutputDirectory.get().asFile,
      includes = includes.files,
      modulePath = projectPath.get(),
    )
}
