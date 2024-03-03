package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.DokkaModuleComponentsSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleParametersKxs
import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.measureTime
import dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
import java.io.File
import javax.inject.Inject
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor

/**
 * Generate multiple Dokka Modules, which must be finalized by compiling them into a Dokka Publication.
 */
abstract class DokkatooGenerateModulesTask2
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
  workers: WorkerExecutor,
  private val fs: FileSystemOperations,
  archives: ArchiveOperations,

  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkatooGenerateTask2(
  objects = objects,
  workers = workers,
  fs = fs,
  archives = archives,
  pluginsConfiguration = pluginsConfiguration,
) {

  /**
   * Directory containing the generation result.
   *
   * Might contain multiple modules, each in a distinct directory determined by the Module path.
   */
  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  /** Dokka Modules directories, containing the output, module descriptor, and module includes. */
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
//  @get:Incremental // TODO make module dirs incremental?
  abstract val dokkaModuleComponentsDirectories: ConfigurableFileCollection

  private val currentProjectDir: File = project.projectDir

//  fun execute(inputChanges: InputChanges) {
//    println(
//      if (inputChanges.isIncremental) "Executing incrementally"
//      else "Executing non-incrementally"
//    )
//
//    inputChanges.getFileChanges(inputDir).forEach { change ->
//      if (change.fileType == FileType.DIRECTORY) return@forEach
//
//      println("${change.changeType}: ${change.normalizedPath}")
//      val targetFile = outputDir.file(change.normalizedPath).get().asFile
//      if (change.changeType == ChangeType.REMOVED) {
//        targetFile.delete()
//      } else {
//        targetFile.writeText(change.file.readText().reversed())
//      }
//    }
//  }

  @TaskAction
  fun action(
//    inputChanges: InputChanges
  ) {
    val outputDirectory = outputDirectory.asFile.get()
    val moduleComponentDirectories = dokkaModuleComponentsDirectories.files

    // clean output dir, so previous generations don't dirty this generation
//    fs.delete { delete(outputDirectory) }
//    outputDirectory.mkdirs()

    val generator = dokkaGeneratorWorker()

    val time = measureTime {
      moduleComponentDirectories.forEach { srcDir ->
        val moduleOutputDir = outputDirectory.resolve(srcDir.toRelativeString(currentProjectDir))
        generateModule(generator, srcDir, moduleOutputDir)
      }
    }

    logger.info("[$path] submitted ${moduleComponentDirectories.size} moduleComponentDirectories in $time")
  }

  private fun generateModule(
    generator: DokkaGeneratorWorker,
    srcDir: File,
    outputDir: File,
  ) {
    val componentsSpec = decodeModuleComponents(srcDir)
    val moduleParameters = decodeModuleParameters(srcDir, componentsSpec)

    // TODO need to get default values for modules from the current project DSL, and override them
    //      with values from the module config.
    val parameters: DokkaGeneratorParametersSpec = generatorParameters
//    parameters.failOnWarning.convention(moduleParameters.failOnWarning)
//    parameters.suppressObviousFunctions.convention(moduleParameters.suppressObviousFunctions)
//    parameters.suppressInheritedMembers.convention(moduleParameters.suppressInheritedMembers)
//    parameters.moduleVersion.convention(moduleParameters.moduleVersion)

    generator.generateModule(
      parameters = parameters,
      outputDirectory = outputDir,
    )
  }

  private fun decodeModuleComponents(
    srcDir: File,
  ): DokkaModuleComponentsSpec {
    val componentsFile = srcDir.resolve(DokkaModuleComponentsSpec.FILE_NAME)
    val componentsText = componentsFile.readText()
    return jsonMapper.decodeFromString(DokkaModuleComponentsSpec.serializer(), componentsText)
  }

  private fun decodeModuleParameters(
    srcDir: File,
    componentsSpec: DokkaModuleComponentsSpec,
  ): DokkaModuleParametersKxs {
    val parametersFile = srcDir.resolve(componentsSpec.moduleParametersFileName)
    val parametersText = parametersFile.readText()
    return jsonMapper.decodeFromString(DokkaModuleParametersKxs.serializer(), parametersText)
  }
}
