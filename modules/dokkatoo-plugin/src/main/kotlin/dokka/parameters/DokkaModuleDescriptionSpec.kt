package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.jetbrains.dokka.DokkaConfiguration

/**
 * Properties that describe a Dokka Module.
 *
 * These values are passed into Dokka Generator, which will aggregate all provided Modules into a
 * single publication.
 */
@DokkatooInternalApi
abstract class DokkaModuleDescriptionSpec
@DokkatooInternalApi
@Inject constructor(
  @get:Input
  val moduleName: String,
) : Named {

  /**
   * Location of the generated Dokka Module.
   *
   * @see DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory
   */
  @get:InputDirectory
  @get:PathSensitive(RELATIVE)
  abstract val moduleDirectory: DirectoryProperty

  /**
   * @see DokkaConfiguration.DokkaModuleDescription.includes
   */
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val includes: ConfigurableFileCollection

  /**
   * File path of the subproject that determines where the Dokka Module will be placed within an
   * assembled Dokka Publication.
   *
   * This must be a relative path, and will be appended to the root Dokka Publication directory.
   *
   * The Gradle project path will also be accepted ([org.gradle.api.Project.getPath]), and the
   * colons `:` will be replaced with file separators `/`.
   */
  @get:Input
  abstract val projectPath: Property<String>

  /**
   * The full Gradle path (e.g. `:a:b:some-subproject:dokkatooGenerateModuleHtml`)
   * of the task that generates this Dokka Module.
   *
   * ugly hack workaround for https://github.com/gradle/gradle/issues/13590
   */
  @get:Internal
  @DokkatooInternalApi
  abstract val moduleGenerateTaskPath: Property<String>

  @Internal
  override fun getName(): String = moduleName
}
