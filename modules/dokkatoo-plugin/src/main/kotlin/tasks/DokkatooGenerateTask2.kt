package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParameters
import dev.adamko.dokkatoo.dokka.parameters.DokkaWorkerParameters
import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
import javax.inject.Inject
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.*
import org.gradle.workers.WorkerExecutor
import org.jetbrains.dokka.DokkaConfiguration

/**
 * Base class for executing Dokka Generator.
 */
abstract class DokkatooGenerateTask2
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
  private val workers: WorkerExecutor,
  private val fs: FileSystemOperations,
  private val archives: ArchiveOperations,

  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkatooTask() {

  /** @see dev.adamko.dokkatoo.dokka.DokkaPublication.enabled */
  @get:Input
  abstract val publicationEnabled: Property<Boolean>

  @get:Nested
  val dokkaGenerator: DokkaGeneratorParameters = objects.newInstance()

  /**
   * The [DokkaConfiguration] by Dokka Generator can be saved to a file for debugging purposes.
   * To disable this behaviour set this property to `null`.
   */
  @DokkatooInternalApi
  @get:Internal
  abstract val dokkaConfigurationJsonFile: RegularFileProperty

  protected fun dokkaGeneratorWorker(): DokkaGeneratorWorker {
    return DokkaGeneratorWorker(
      runtimeClasspath = dokkaGenerator.runtimeClasspath,
      isolation = dokkaGenerator.isolation.get(),
//      generatorParameters = generatorParameters,
      cacheDirectory = dokkaGenerator.cacheDirectory.asFile.orNull,
      workerLogFile = dokkaGenerator.logFile.asFile.get(),
//      dokkaConfigurationJsonFile = dokkaConfigurationJsonFile.asFile.orNull,

      taskPath = path,
      workers = workers,
      archives = archives,
    )
  }
}
