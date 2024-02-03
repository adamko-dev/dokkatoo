package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
import dev.adamko.dokkatoo.dokka.parameters.builders.DokkaParametersBuilder
import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.workers.ClassLoaderIsolation
import dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
import dev.adamko.dokkatoo.workers.ProcessIsolation
import dev.adamko.dokkatoo.workers.WorkerIsolation
import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.json.JsonElement
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.workers.WorkerExecutor
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.toPrettyJsonString

/**
 * Executes the Dokka Generator, and produces documentation.
 *
 * The type of documentation generated is determined by the supplied Dokka Plugins in [generator].
 */
@CacheableTask
abstract class DokkatooGenerateTask
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
  private val workers: WorkerExecutor,

  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkatooTask() {

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  /**
   * Classpath required to run Dokka Generator.
   *
   * Contains the Dokka Generator, Dokka plugins, and any transitive dependencies.
   */
  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:LocalState
  abstract val cacheDirectory: DirectoryProperty

  /**
   * Generating a Dokka Module? Set this to [GenerationType.MODULE].
   *
   * Generating a Dokka Publication? [GenerationType.PUBLICATION].
   */
  @get:Input
  abstract val generationType: Property<GenerationType>

  /** @see dev.adamko.dokkatoo.dokka.DokkaPublication.enabled */
  @get:Input
  abstract val publicationEnabled: Property<Boolean>

  @get:Nested
  val generator: DokkaGeneratorParametersSpec = objects.newInstance(pluginsConfiguration)

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

  @get:Internal
  abstract val workerLogFile: RegularFileProperty

  /**
   * The [DokkaConfiguration] by Dokka Generator can be saved to a file for debugging purposes.
   * To disable this behaviour set this property to `null`.
   */
  @DokkatooInternalApi
  @get:Internal
  abstract val dokkaConfigurationJsonFile: RegularFileProperty

  enum class GenerationType {
    MODULE,
    PUBLICATION,
  }

  @TaskAction
  internal fun generateDocumentation() {
    val dokkaConfiguration = createDokkaConfiguration()
    logger.info("dokkaConfiguration: $dokkaConfiguration")
    dumpDokkaConfigurationJson(dokkaConfiguration)

    logger.info("DokkaGeneratorWorker runtimeClasspath: ${runtimeClasspath.asPath}")

    val isolation = workerIsolation.get()
    logger.info("[$path] running with workerIsolation $isolation")
    val workQueue = when (isolation) {
      is ClassLoaderIsolation ->
        workers.classLoaderIsolation {
          classpath.from(runtimeClasspath)
        }

      is ProcessIsolation     ->
        workers.processIsolation {
          classpath.from(runtimeClasspath)
          forkOptions {
            isolation.defaultCharacterEncoding.orNull?.let(this::setDefaultCharacterEncoding)
            isolation.debug.orNull?.let(this::setDebug)
            isolation.enableAssertions.orNull?.let(this::setEnableAssertions)
            isolation.maxHeapSize.orNull?.let(this::setMaxHeapSize)
            isolation.minHeapSize.orNull?.let(this::setMinHeapSize)
            isolation.jvmArgs.orNull?.let(this::setJvmArgs)
            isolation.systemProperties.orNull?.let(this::systemProperties)
          }
        }
    }

    workQueue.submit(DokkaGeneratorWorker::class) {
      this.dokkaParameters.set(dokkaConfiguration)
      this.logFile.set(workerLogFile)
    }
  }

  /**
   * Dump the [DokkaConfiguration] JSON to a file ([dokkaConfigurationJsonFile]) for debugging
   * purposes.
   */
  private fun dumpDokkaConfigurationJson(
    dokkaConfiguration: DokkaConfiguration,
  ) {
    val destFile = dokkaConfigurationJsonFile.asFile.orNull ?: return
    destFile.parentFile.mkdirs()
    destFile.createNewFile()

    val compactJson = dokkaConfiguration.toPrettyJsonString()
    val json = jsonMapper.decodeFromString(JsonElement.serializer(), compactJson)
    val prettyJson = jsonMapper.encodeToString(JsonElement.serializer(), json)

    destFile.writeText(prettyJson)

    logger.info("[$path] Dokka Generator configuration JSON: ${destFile.toURI()}")
  }

  private fun createDokkaConfiguration(): DokkaConfiguration {
    val outputDirectory = outputDirectory.get().asFile

    val delayTemplateSubstitution = when (generationType.orNull) {
      GenerationType.MODULE      -> true
      GenerationType.PUBLICATION -> false
      null                       -> error("missing GenerationType")
    }

    return DokkaParametersBuilder.build(
      spec = generator,
      delayTemplateSubstitution = delayTemplateSubstitution,
      outputDirectory = outputDirectory,
      moduleDescriptors = generator.moduleDescriptors,
      cacheDirectory = cacheDirectory.asFile.orNull,
    )
  }

  //region Deprecated Properties
  /**
   * Please move worker options:
   *
   * ```kotlin
   * // build.gradle.kts
   *
   * dokkatoo {
   *   dokkaGeneratorIsolation = ProcessIsolation {
   *     debug = true
   *   }
   * }
   * ```
   *
   * Worker options were moved to allow for configuring worker isolation.
   *
   * @see dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation
   * @see dev.adamko.dokkatoo.workers.ProcessIsolation.debug
   */
  @get:Internal
  @Deprecated("Please move worker options to `DokkatooExtension.dokkaGeneratorIsolation`. Worker options were moved to allow for configuring worker isolation")
  abstract val workerDebugEnabled: Property<Boolean>
  /**
   * Please move worker options:
   *
   * ```kotlin
   * // build.gradle.kts
   *
   * dokkatoo {
   *   dokkaGeneratorIsolation = ProcessIsolation {
   *     minHeapSize = "512m"
   *   }
   * }
   * ```
   *
   * Worker options were moved to allow for configuring worker isolation.
   *
   * @see dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation
   * @see dev.adamko.dokkatoo.workers.ProcessIsolation.minHeapSize
   */
  @get:Internal
  @Deprecated("Please move worker options to `DokkatooExtension.dokkaGeneratorIsolation`. Worker options were moved to allow for configuring worker isolation")
  abstract val workerMinHeapSize: Property<String>
  /**
   * Please move worker options:
   *
   * ```kotlin
   * // build.gradle.kts
   *
   * dokkatoo {
   *   dokkaGeneratorIsolation = ProcessIsolation {
   *     maxHeapSize = "1g"
   *   }
   * }
   * ```
   *
   * Worker options were moved to allow for configuring worker isolation.
   *
   * @see dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation
   * @see dev.adamko.dokkatoo.workers.ProcessIsolation.maxHeapSize
   */
  @get:Internal
  @Deprecated("Please move worker options to `DokkatooExtension.dokkaGeneratorIsolation`. Worker options were moved to allow for configuring worker isolation")
  abstract val workerMaxHeapSize: Property<String>
  /**
   * Please move worker options:
   *
   * ```kotlin
   * // build.gradle.kts
   *
   * dokkatoo {
   *   dokkaGeneratorIsolation = ProcessIsolation {
   *     jvmArgs = listOf(
   *       "-XX:+UseStringDeduplication",
   *     )
   *   }
   * }
   * ```
   *
   * Worker options were moved to allow for configuring worker isolation.
   *
   * @see dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation
   * @see dev.adamko.dokkatoo.workers.ProcessIsolation.jvmArgs
   */
  @get:Internal
  @Deprecated("Please move worker options to `DokkatooExtension.dokkaGeneratorIsolation`. Worker options were moved to allow for configuring worker isolation")
  abstract val workerJvmArgs: ListProperty<String>
  //endregion
}
