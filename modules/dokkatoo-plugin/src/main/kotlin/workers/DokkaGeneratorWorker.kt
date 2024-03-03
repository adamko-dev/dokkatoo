package dev.adamko.dokkatoo.workers

import dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
import dev.adamko.dokkatoo.dokka.parameters.builders.DokkaParametersBuilder
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.File
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.*
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.dokka.DokkaConfiguration

@DokkatooInternalApi
class DokkaGeneratorWorker(
  private val runtimeClasspath: FileCollection,
  private val isolation: WorkerIsolation,

//  private val generatorParameters: DokkaGeneratorParametersSpec,
  private val cacheDirectory: File?,

  private val workerLogFile: File?,
//  private val dokkaConfigurationJsonFile: File?,

  taskPath: String,
  workers: WorkerExecutor,
  archives: ArchiveOperations,
) {

  private val logTag = "[$taskPath:DokkaGeneratorWorker]"
  private val dokkaParametersBuilder = DokkaParametersBuilder(archives)

  private val workQueue: WorkQueue = workers.dokkaGeneratorQueue()

  internal fun generateModule(
    parameters: DokkaGeneratorParametersSpec,
    outputDirectory: File,
  ) {
    generate(
      parameters = parameters,
      delayTemplateSubstitution = true,
      dokkaModuleDirectories = emptyList(),
      outputDirectory = outputDirectory,
    )
  }

  internal fun generatePublication(
    parameters: DokkaGeneratorParametersSpec,
    dokkaModuleDirectories: Collection<File>,
    outputDirectory: File,
  ) {
    generate(
      parameters = parameters,
      delayTemplateSubstitution = false,
      dokkaModuleDirectories = dokkaModuleDirectories,
      outputDirectory = outputDirectory,
    )
  }

  /**
   * Asynchronously start generating a Dokka Module or Publication into [outputDirectory]
   */
  private fun generate(
    parameters: DokkaGeneratorParametersSpec,
    delayTemplateSubstitution: Boolean,
    dokkaModuleDirectories: Collection<File>,
    outputDirectory: File,
  ) {
    val dokkaConfiguration = createDokkaConfiguration(
      parameters = parameters,
      delayTemplateSubstitution = delayTemplateSubstitution,
      dokkaModuleDirectories = dokkaModuleDirectories,
      outputDirectory = outputDirectory,
    )
    logger.info("$logTag dokkaConfiguration: $dokkaConfiguration")
//    dumpDokkaConfigurationJson(dokkaConfiguration)

    workQueue.submit(DokkaGeneratorWorkAction::class) {
      this.dokkaParameters.set(dokkaConfiguration)
      this.logFile.set(workerLogFile)
    }
  }

  private fun WorkerExecutor.dokkaGeneratorQueue(): WorkQueue {
    logger.info("$logTag runtimeClasspath: ${runtimeClasspath.asPath}")

    logger.info("$logTag running with workerIsolation $isolation")

    return when (isolation) {
      is ClassLoaderIsolation ->
        classLoaderIsolation {
          classpath.from(runtimeClasspath)
        }

      is ProcessIsolation     ->
        processIsolation {
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
  }


  private fun createDokkaConfiguration(
    parameters: DokkaGeneratorParametersSpec,
    delayTemplateSubstitution: Boolean,
    dokkaModuleDirectories: Collection<File>,
    outputDirectory: File,
  ): DokkaConfiguration {

    val moduleOutputDirectories = dokkaModuleDirectories.toList()
    logger.info("$logTag got ${moduleOutputDirectories.size} moduleOutputDirectories: $moduleOutputDirectories")

    return dokkaParametersBuilder.build(
      spec = parameters,
      delayTemplateSubstitution = delayTemplateSubstitution,
      outputDirectory = outputDirectory,
      moduleDescriptorDirs = moduleOutputDirectories,
      cacheDirectory = cacheDirectory,
    )
  }


//  /**
//   * Dump the [DokkaConfiguration] JSON to a file ([dokkaConfigurationJsonFile]) for debugging
//   * purposes.
//   */
//  private fun dumpDokkaConfigurationJson(
//    dokkaConfiguration: DokkaConfiguration,
//  ) {
//    val destFile = dokkaConfigurationJsonFile ?: return
//    destFile.parentFile.mkdirs()
//    destFile.createNewFile()
//
//    val compactJson = dokkaConfiguration.toPrettyJsonString()
//    val json = DokkatooBasePlugin.jsonMapper.decodeFromString(JsonElement.serializer(), compactJson)
//    val prettyJson = DokkatooBasePlugin.jsonMapper.encodeToString(JsonElement.serializer(), json)
//
//    destFile.writeText(prettyJson)
//
//    logger.info("$logTag Dokka Generator configuration JSON: ${destFile.toURI()}")
//  }

  @DokkatooInternalApi
  companion object {
    private val logger: Logger = Logging.getLogger(DokkaGeneratorWorker::class.java)
  }
}
