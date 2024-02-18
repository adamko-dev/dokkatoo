package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.DokkaModuleComponentsSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleParametersKxs
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleParametersKxs.*
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleParametersKxs.SourceSetIdKxs.Companion.resolve
import dev.adamko.dokkatoo.dokka.parameters.builders.DokkaModuleParametersBuilder
import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.mapToSet
import java.io.File
import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*

/**
 * Collect the components required to generate a Dokka Module.
 *
 * This task is only necessary because Gradle is buggy (sharing files between subprojects doesn't
 * work correctly). Maybe one day it can be removed...
 */
@CacheableTask
abstract class PrepareDokkaModuleComponentsTask
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
  private val fs: FileSystemOperations,
//  private val archives: ArchiveOperations,
//  private val workers: WorkerExecutor,

  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  private val pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkatooTask() {
  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @get:Nested
  val generatorParameters: DokkaGeneratorParametersSpec = objects.newInstance(pluginsConfiguration)

  @TaskAction
  fun action() {
    val componentsDir = outputDirectory.get().asFile
    println("[$path] componentsDir:$componentsDir")

    // clear output directory to prevent previous executions corrupting this execution
    fs.delete { delete(outputDirectory) }
    outputDirectory.get().asFile.mkdirs()

    val componentsSpec = createModuleComponents(
      componentsDir = componentsDir,
    )
    println("[$path] componentsSpec:$componentsSpec")

    val parameters = convertModuleParameters(
      componentsDir = componentsDir,
//      componentsSpec = moduleComponents,
    )
    println("[$path] generatorParameters:  $generatorParameters")
    println("[$path]     .dokkaSourceSets: ${generatorParameters.dokkaSourceSets.toList()}")
    println("[$path] parameters:$parameters")


    syncFiles(
      componentsDir = componentsDir,
      componentsSpec = componentsSpec,
      parameters = parameters,
    )
  }

  private fun createModuleComponents(
    componentsDir: File,
  ): DokkaModuleComponentsSpec {
    return DokkaModuleComponentsSpec(
      sourceSetDirNames = generatorParameters.dokkaSourceSets.associate { dss ->
        val dssId = SourceSetIdKxs(dss.sourceSetId.get())
        val sourceSetDir = componentsDir.resolve(dssId)
        val sourceSetRelativePath = sourceSetDir.relativeTo(componentsDir).invariantSeparatorsPath
        dssId.key to sourceSetRelativePath
      }
    )
  }

  private fun convertModuleParameters(
    componentsDir: File,
//    componentsSpec: DokkaModuleComponentsSpec,
  ): DokkaModuleParametersKxs {
    val parameters = DokkaModuleParametersKxs(
      moduleName = generatorParameters.moduleName.get(),
      moduleVersion = generatorParameters.moduleVersion.orNull,
      failOnWarning = generatorParameters.failOnWarning.get(),
      sourceSets = generatorParameters.dokkaSourceSets.map { dss ->

//        val sourceSetDirName = componentsSpec.sourceSetDirName(dss.sourceSetId.get())

        val dssId = SourceSetIdKxs(dss.sourceSetId.get())
//        val sourceSetDir = outputDirectory.resolve(dssId)

        DokkaSourceSetKxs(
          sourceSetId = dssId,
          displayName = dss.displayName.get(),
          dependentSourceSetIds = dss.dependentSourceSets
            .mapToSet { spec -> SourceSetIdKxs(spec) },
          reportUndocumented = dss.reportUndocumented.get(),
          skipEmptyPackages = dss.skipEmptyPackages.get(),
          skipDeprecated = dss.skipDeprecated.get(),
          jdkVersion = dss.jdkVersion.get(),
          sourceLinks = dss.sourceLinks.mapToSet { spec ->
            SourceLinkDefinitionKxs(
              spec = spec,
              componentsDir = componentsDir,
            )
          },
          perPackageOptions = dss.perPackageOptions.map(::PackageOptionsKxs),
          externalDocumentationLinks = dss.externalDocumentationLinks.mapToSet { spec ->
            ExternalDocumentationLinkKxs(spec)
          },
          languageVersion = dss.languageVersion.orNull,
          apiVersion = dss.apiVersion.orNull,
          enableKotlinStdLibDocumentationLink = dss.enableKotlinStdLibDocumentationLink.get(),
          enableJdkDocumentationLink = dss.enableJdkDocumentationLink.get(),
          analysisPlatform = dss.analysisPlatform.get(),
          documentedVisibilities = dss.documentedVisibilities.get(),
          sourceRootsRelativePaths = dss.sourceRoots
            .map { sourceRoot ->
              sourceRoot.relativeTo(componentsDir).invariantSeparatorsPath
            },
          suppressedFileRelativePaths = dss.suppressedFiles
            .map { suppressedFile ->
              suppressedFile.relativeTo(componentsDir).invariantSeparatorsPath
            }
            .toSet(),
        )
      },
      pluginsConfiguration = pluginsConfiguration.map { spec -> PluginParametersKxs(spec) },
      suppressObviousFunctions = generatorParameters.suppressObviousFunctions.get(),
      suppressInheritedMembers = generatorParameters.suppressInheritedMembers.get(),
      finalizeCoroutines = generatorParameters.finalizeCoroutines.get(),
    )

    return parameters
  }


  private fun syncFiles(
    componentsDir: File,
    componentsSpec: DokkaModuleComponentsSpec,
    parameters: DokkaModuleParametersKxs,
  ) {
    fs.sync {
      into(componentsDir)

      generatorParameters.dokkaSourceSets.forEach { dss ->
        val dssDirName = componentsSpec.sourceSetDirName(dss.sourceSetId.get())
        val dssPath = "${componentsSpec.sourceSetsDirName}/${dssDirName}"

        from(dss.includes) {
          into("$dssPath/${componentsSpec.includesDirName}")
        }

        from(dss.samples) {
          into("$dssPath/${componentsSpec.samplesDirName}")
        }

        from(dss.classpath) {
          into("$dssPath/${componentsSpec.classpathDirName}")
        }
      }

      from(generatorParameters.includes) {
        into(componentsSpec.includesDirName)
      }
    }


    encodeComponentsSpec(
      componentsDir = componentsDir,
      componentsSpec = componentsSpec,
    )
    encodeModuleParameters(
      componentsDir = componentsDir,
      componentsSpec = componentsSpec,
      parameters = parameters,
    )
  }

  private fun encodeComponentsSpec(
    componentsDir: File,
    componentsSpec: DokkaModuleComponentsSpec,
  ) {
    val encodedComponents =
      jsonMapper.encodeToString(DokkaModuleComponentsSpec.serializer(), componentsSpec)

    componentsDir.resolve(DokkaModuleComponentsSpec.FILE_NAME).apply {
      parentFile.mkdirs()
      writeText(encodedComponents)
      println("[$path] encoded componentsSpec: $this")
    }
  }

  private fun encodeModuleParameters(
    componentsDir: File,
    componentsSpec: DokkaModuleComponentsSpec,
    parameters: DokkaModuleParametersKxs,
  ): DokkaModuleParametersKxs {
    val encodedParameters =
      jsonMapper.encodeToString(DokkaModuleParametersKxs.serializer(), parameters)

    componentsDir.resolve(componentsSpec.moduleParametersFileName).apply {
      parentFile.mkdirs()
      writeText(encodedParameters)
      println("[$path] encoded parameters: $this")
    }

    return parameters
  }
}
