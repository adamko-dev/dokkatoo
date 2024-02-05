package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor

@CacheableTask
abstract class DokkatooGeneratePublicationTask
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
  private val workers: WorkerExecutor,

  private val fs: FileSystemOperations,
  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkatooGenerateTask(
  objects = objects,
  workers = workers,
  pluginsConfiguration = pluginsConfiguration,
) {

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @TaskAction
  internal fun generatePublication() {
    val outputDirectory = outputDirectory.get().asFile

    // clean output dir, so previous generations don't dirty this generation
    fs.delete { delete(outputDirectory) }
    outputDirectory.mkdirs()

    // run Dokka Generator
    generateDocumentation(GeneratorMode.Publication, outputDirectory)
  }
}
