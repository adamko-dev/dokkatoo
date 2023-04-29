package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
import javax.inject.Inject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.*
import org.gradle.process.JavaForkOptions
import org.gradle.workers.WorkerExecutor

/**
 * Executes the Dokka Generator, and produces documentation.
 *
 * The type of documentation generated is determined by the supplied Dokka Plugins in [dokkaParametersJson].
 */
@CacheableTask
abstract class DokkatooGenerateTask
@DokkatooInternalApi
@Inject
constructor(
  private val workers: WorkerExecutor,
) : DokkatooTask.WithSourceSets() {

  @get:InputFile
  @get:PathSensitive(NAME_ONLY)
  abstract val dokkaParametersJson: RegularFileProperty

  /**
   * Contains both the Dokka Generator classpath and Dokka plugins, and any transitive dependencies
   * of either.
   */
  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:LocalState
  abstract val cacheDirectory: DirectoryProperty

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  /**
   * Generating a Dokka Module? Set this to [GenerationType.MODULE].
   *
   * Generating a Dokka Publication? [GenerationType.PUBLICATION].
   */
  @get:Input
  abstract val generationType: Property<GenerationType>

  @get:InputFiles
  @get:Optional
  @get:PathSensitive(RELATIVE)
  abstract val dokkaModuleFiles: ConfigurableFileCollection

  /** @see JavaForkOptions.getDebug */
  @get:Input
  abstract val workerDebugEnabled: Property<Boolean>
  /** @see JavaForkOptions.getMinHeapSize */
  @get:Input
  abstract val workerMinHeapSize: Property<String>
  /** @see JavaForkOptions.getMaxHeapSize */
  @get:Input
  abstract val workerMaxHeapSize: Property<String>
  /** @see JavaForkOptions.jvmArgs */
  @get:Input
  abstract val workerJvmArgs: ListProperty<String>

  @get:Internal
  abstract val workerLogFile: RegularFileProperty

  enum class GenerationType {
    MODULE,
    PUBLICATION,
  }

  @TaskAction
  @OptIn(ExperimentalSerializationApi::class) // jsonMapper.decodeFromStream
  internal fun generateDocumentation() {
    val dokkaParametersJsonFile = dokkaParametersJson.get().asFile
    val generationType: GenerationType = generationType.get()
    val outputDirectory = outputDirectory.get().asFile

    val dokkaParameters = jsonMapper.decodeFromStream(
      DokkaParametersKxs.serializer(),
      dokkaParametersJsonFile.inputStream(),
    ).copy(
      delayTemplateSubstitution = when (generationType) {
        GenerationType.MODULE      -> true
        GenerationType.PUBLICATION -> false
      },
    )

    logger.info("dokkaParameters: $dokkaParameters")

    dokkaParameters.modules.forEach {
      // workaround until https://github.com/Kotlin/dokka/pull/2867 is released
      this.outputDirectory.dir(it.modulePath).get().asFile.mkdirs()
    }

    logger.info("DokkaGeneratorWorker runtimeClasspath: ${runtimeClasspath.asPath}")

    val workQueue = workers.processIsolation {
      classpath.from(runtimeClasspath)
      forkOptions {
        defaultCharacterEncoding = "UTF-8"
        minHeapSize = workerMinHeapSize.get()
        maxHeapSize = workerMaxHeapSize.get()
        enableAssertions = true
        debug = workerDebugEnabled.get()
        jvmArgs = workerJvmArgs.get()
      }
    }

    val dokkaParametersImpl = dokkaParameters.convert(outputDirectory)

    workQueue.submit(DokkaGeneratorWorker::class) {
      this.dokkaParameters.set(dokkaParametersImpl)
      this.logFile.set(workerLogFile)
    }
  }
}
