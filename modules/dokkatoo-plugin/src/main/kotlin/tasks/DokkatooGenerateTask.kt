package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
import dev.adamko.dokkatoo.dokka.parameters.builders.DokkaParametersBuilder
import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.adding
import dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
import java.io.IOException
import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.process.JavaForkOptions
import org.gradle.workers.WorkerExecutor
import org.jetbrains.dokka.DokkaConfiguration

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
  objects: ObjectFactory,
  private val workers: WorkerExecutor,

  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkatooTask() {

  @get:Internal
  @Deprecated("removing DokkatooPrepareParametersTask")
  abstract val dokkaParametersJson: RegularFileProperty

  /**
   * Classpath required to run Dokka Generator.
   *
   * Contains the Dokka Generator, Dokka plugins, and any transitive dependencies.
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

  /** @see dev.adamko.dokkatoo.dokka.DokkaPublication.enabled */
  @get:Input
  abstract val publicationEnabled: Property<Boolean>

  @get:Nested
  val generator: DokkaGeneratorParametersSpec =
    extensions.adding("generator", objects.newInstance(pluginsConfiguration))

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
  internal fun generateDocumentation() {
    val dokkaConfiguration = createDokkaConfiguration()
    logger.info("dokkaConfiguration: $dokkaConfiguration")

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

    workQueue.submit(DokkaGeneratorWorker::class) {
      this.dokkaParameters.set(dokkaConfiguration)
      this.logFile.set(workerLogFile)
    }
  }

  private fun createDokkaConfiguration(): DokkaConfiguration {
    val outputDirectory = outputDirectory.get().asFile

    val delayTemplateSubstitution = when (generationType.orNull) {
      GenerationType.MODULE      -> true
      GenerationType.PUBLICATION -> false
      null                       -> error("missing GenerationType")
    }

    val dokkaModuleDescriptors = generator.dokkaModules
    dokkaModuleDescriptors().forEach {
      // workaround until https://github.com/Kotlin/dokka/pull/2867 is released
      this.outputDirectory.dir(it.modulePath).get().asFile.mkdirs()
    }

//    val moduleDescriptionFiles: Map<String, DokkaModuleDescriptionKxs.Files> =
//      emptyMap() // TODO...

    return DokkaParametersBuilder.build(
      spec = generator,
      delayTemplateSubstitution = delayTemplateSubstitution,
      outputDirectory = outputDirectory,
      modules = dokkaModuleDescriptors,
      cacheDirectory = cacheDirectory.asFile.orNull,
    )
  }

  private fun dokkaModuleDescriptors(): List<DokkaModuleDescriptionKxs> {
    return generator.dokkaModules//.asFileTree
//      .matching { include("**/module_descriptor.json") }
      //.files
      .map { spec ->
        val file = spec.moduleDescriptorJson.asFile.get()
        try {
          val fileContent = file.readText()
          jsonMapper.decodeFromString(
            DokkaModuleDescriptionKxs.serializer(),
            fileContent,
          )
        } catch (ex: Exception) {
          throw IOException("Could not parse DokkaModuleDescriptionKxs from $file", ex)
        }
      }
  }

  //region Deprecated Properties
  @Suppress("unused")
  @get:Internal
  @Deprecated("moved to nested property", ReplaceWith("generator.dokkaModuleFiles"))
  val dokkaModuleFiles: ConfigurableFileCollection
    get() = objects.fileCollection()
  //endregion
}
