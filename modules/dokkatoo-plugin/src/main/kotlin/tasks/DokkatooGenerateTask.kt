package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs
import dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
import javax.inject.Inject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkerExecutor

/**
 * Executes the Dokka Generator, and produces documentation.
 *
 * The type of documentation generated is determined by the supplied Dokka Plugins in [dokkaParametersJson].
 */
@CacheableTask
abstract class DokkatooGenerateTask @Inject constructor(
  private val workers: WorkerExecutor,
) : DokkatooTask() {

  @get:InputFile
  @get:PathSensitive(RELATIVE)
  abstract val dokkaParametersJson: RegularFileProperty

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

//    @get:Classpath
//    abstract val pluginClasspath: ConfigurableFileCollection

  @get:LocalState
  abstract val cacheDirectory: DirectoryProperty

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  /**
   * Generating a Dokka Module? Set this to `true`.
   *
   * Generating a Dokka Publication? `false`.
   */
  @get:Input
  abstract val generationType: Property<GenerationType>

  @get:InputFiles
  @get:Optional
  @get:PathSensitive(RELATIVE)
  abstract val dokkaModuleSourceDirectories: ConfigurableFileCollection

  enum class GenerationType {
    MODULE,
    PUBLICATION,
  }

  @TaskAction
  @OptIn(ExperimentalSerializationApi::class) // jsonMapper.decodeFromStream
  fun generateDocumentation() {
    val dokkaParametersJsonFile = dokkaParametersJson.get().asFile
    val generationType =
      generationType.orNull ?: error("GenerationType is required, but no value was provided")

    val dokkaParameters = jsonMapper.decodeFromStream(
      DokkaParametersKxs.serializer(),
      dokkaParametersJsonFile.inputStream(),
    ).copy(
      outputDir = outputDirectory.get().asFile,
      delayTemplateSubstitution = when (generationType) {
        GenerationType.MODULE      -> true
        GenerationType.PUBLICATION -> false
      },
    )

    logger.info("dokkaParameters: $dokkaParameters")

    logger.info("DokkaGeneratorWorker runtimeClasspath: ${runtimeClasspath.files.joinToString("\n") { it.name }}")

    val workQueue = workers.processIsolation {
      classpath.from(runtimeClasspath)
//            classpath.from(pluginClasspath)
      forkOptions {
        defaultCharacterEncoding = "UTF-8"
      }
    }

    workQueue.submit(DokkaGeneratorWorker::class) worker@{
      this@worker.dokkaParameters.set(dokkaParameters)
    }
  }

//    /**
//     * Extract a property from [dokkaConfigurationJson] so it can be converted to a specific Gradle property type
//     * (for example, a [DirectoryProperty]) and used as a task property.
//     */
//    internal inline fun <reified T : Any> dokkaConfigurationValue(
//        crossinline property: DokkaConfigurationKxs.() -> T?
//    ): Provider<T> {
//        return providers.fileContents(dokkaConfigurationJson).asText.mapNotNull { dokkaConfigurationJson ->
//
//            val dokkaConfiguration = jsonMapper.decodeFromString(
//                DokkaConfigurationKxs.serializer(),
//                dokkaConfigurationJson,
//            )
//
//            dokkaConfiguration.property()
//        }
//    }
//
//    // workaround for https://github.com/gradle/gradle/issues/12388
//    private fun <T, R> Provider<T>.mapNotNull(map: (value: T) -> R?): Provider<R> =
//        flatMap { value -> providers.provider { map(value) } }
}
