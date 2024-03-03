package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParameters
import dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
import dev.adamko.dokkatoo.workers.WorkerIsolation
import javax.inject.Inject
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
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
    val generatorParameters: DokkaGeneratorParameters = objects.newInstance()

  @get:Nested
  val generatorParameters: DokkaGeneratorParametersSpec = objects.newInstance(pluginsConfiguration)

  /**
   * The [DokkaConfiguration] by Dokka Generator can be saved to a file for debugging purposes.
   * To disable this behaviour set this property to `null`.
   */
  @DokkatooInternalApi
  @get:Internal
  abstract val dokkaConfigurationJsonFile: RegularFileProperty

  /**
   * Control whether Dokkatoo launches Dokka Generator.
   *
   * Defaults to [dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation].
   *
   * @see dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation
   * @see dev.adamko.dokkatoo.workers.ProcessIsolation
   */
  @get:Nested
  abstract val workerIsolation: Property<WorkerIsolation>

  /**
   * Classpath required to run Dokka Generator.
   *
   * Contains the Dokka Generator, Dokka plugins, and any transitive dependencies.
   */
  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:Internal
  abstract val workerLogFile: RegularFileProperty

  @get:LocalState
  abstract val cacheDirectory: DirectoryProperty

  protected fun dokkaGeneratorWorker(): DokkaGeneratorWorker {
    return DokkaGeneratorWorker(
      runtimeClasspath = runtimeClasspath,
      isolation = workerIsolation.get(),
//      generatorParameters = generatorParameters,
      cacheDirectory = cacheDirectory.asFile.orNull,
      workerLogFile = workerLogFile.asFile.get(),
//      dokkaConfigurationJsonFile = dokkaConfigurationJsonFile.asFile.orNull,

      taskPath = path,
      workers = workers,
      archives = archives,
    )
  }

  //region deprecated properties
  @get:ReplacedBy("generatorParameters")
  @Deprecated("renamed", ReplaceWith("generatorParameters"))
  val generator: DokkaGeneratorParametersSpec
    get() = generatorParameters
  //endregion
}
