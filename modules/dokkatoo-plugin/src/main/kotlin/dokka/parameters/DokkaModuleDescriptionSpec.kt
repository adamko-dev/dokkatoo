package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

@DokkatooInternalApi
abstract class DokkaModuleDescriptionSpec
@DokkatooInternalApi
@Inject constructor(
  @get:Input
  val moduleName: String,
) : DokkaParameterBuilder<DokkaParametersKxs.DokkaModuleDescriptionKxs>, Named {

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
