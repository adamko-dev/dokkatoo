package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE

/**
 * Produces a Dokka Configuration that describes a single module of a multimodule Dokka configuration.
 *
 * @see dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
 */
@CacheableTask
abstract class DokkatooPrepareModuleIncludesTask
@DokkatooInternalApi
@Inject
constructor(
  private val fs: FileSystemOperations,
) : DokkatooTask() {

  @get:InputFiles
  @get:Optional
  @get:PathSensitive(RELATIVE)
  abstract val includes: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val destinationDir: DirectoryProperty

  @TaskAction
  internal fun generateModuleConfiguration() {
    fs.sync {
      from(includes)
      into(destinationDir)
    }
  }
}
