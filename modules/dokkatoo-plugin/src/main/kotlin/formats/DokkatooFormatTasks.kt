package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkaComponentArtifactType.*
import dev.adamko.dokkatoo.distributions.FormatDependenciesManager
import dev.adamko.dokkatoo.distributions.FormatDependencyContainers
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionSpec
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.configuring
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import dev.adamko.dokkatoo.tasks.DokkatooPrepareModuleDescriptorTask
import dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask
import java.io.IOException
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*


@DokkatooInternalApi
class DokkatooFormatTasks(
  tasks: TaskContainer,
  private val publication: DokkaPublication,
  private val dokkatooExtension: DokkatooExtension,
  private val dependencyContainers: FormatDependencyContainers,
  private val dependenciesManager: FormatDependenciesManager,

  private val providers: ProviderFactory,
) {
  private val formatName: String get() = publication.formatName

  private val taskNames = DokkatooBasePlugin.TaskNames(formatName)

  @Suppress("DEPRECATION", "unused")
  @Deprecated("No longer required")
  val prepareParameters: TaskProvider<DokkatooPrepareParametersTask>

  val generatePublication: TaskProvider<DokkatooGenerateTask>

  val generateModule: TaskProvider<DokkatooGenerateTask>

  val prepareModuleDescriptor: TaskProvider<DokkatooPrepareModuleDescriptorTask>

  init {
    @Suppress("DEPRECATION")
    prepareParameters = tasks.register<DokkatooPrepareParametersTask>(
      taskNames.prepareParameters,
    ).configuring task@{
      description =
        "[DEPRECATED no longer used] Prepares Dokka parameters for generating the $formatName publication"

      dokkaConfigurationJson.convention(
        dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/dokka_parameters.json")
      )
    }


    generatePublication = tasks.register<DokkatooGenerateTask>(
      taskNames.generatePublication,
      publication.pluginsConfiguration,
    ).configuring task@{
      description = "Executes the Dokka Generator, generating the $formatName publication"
      generationType.set(DokkatooGenerateTask.GenerationType.PUBLICATION)

      outputDirectory.convention(dokkatooExtension.dokkatooPublicationDirectory.dir(formatName))

      generator.apply {
        // depend on Dokka Module Descriptors from other subprojects
//        dokkaModuleFiles.from(
//          dependenciesManager.consumeDokkatooModuleFiles(
//            ModuleDescriptorJson,
//          )
////          dependencyContainers.dokkaModuleConsumer.map { modules ->
////            modules.incoming
////              .artifactView { componentFilter(LocalProjectOnlyFilter) }
////              .artifacts.artifactFiles
////          }
//        )

        val moduleIncludes: Provider<Map<String?, FileCollection>> =
          dependenciesManager.consumeDokkatooModuleFilesGroupedByName(Includes)
        val moduleSourceOutputDirectory: Provider<Map<String?, FileCollection>> =
          dependenciesManager.consumeDokkatooModuleFilesGroupedByName(SourceOutputDirectory)

        dokkaModules.addAllLater(
          dependenciesManager.consumeDokkatooModuleFiles(
            ModuleDescriptorJson,
          ).map { jsonFiles ->
            jsonFiles.map { file ->
              try {
                val fileContent = file.readText()
                DokkatooBasePlugin.jsonMapper.decodeFromString(
                  DokkaModuleDescriptionKxs.serializer(),
                  fileContent,
                )
              } catch (ex: Exception) {
                throw IOException("Could not parse DokkaModuleDescriptionKxs from $file", ex)
              }
            }.map { descriptor ->
              objects.newInstance<DokkaModuleDescriptionSpec>(descriptor.name).apply {
                projectPath.set(descriptor.modulePath)
                includes.from(
                  moduleIncludes.map { it[descriptor.name] ?: objects.fileCollection() }
                )
                sourceOutputDirectory.set(
                  objects.fileProperty().fileProvider(
                    moduleSourceOutputDirectory.map {
                      it[descriptor.name]?.singleFile
                        ?: error("missing source output dir for ${descriptor.name}")
                    }
                  )
                )
                includes.from(
                  moduleIncludes.map { it[descriptor.name] ?: objects.fileCollection() }
                )
              }
            }
          })
      }

      applyFormatSpecificConfiguration()
    }


    generateModule = tasks.register<DokkatooGenerateTask>(
      taskNames.generateModule,
      publication.pluginsConfiguration,
    ).configuring task@{
      description = "Executes the Dokka Generator, generating a $formatName module"
      generationType.set(DokkatooGenerateTask.GenerationType.MODULE)

      outputDirectory.convention(dokkatooExtension.dokkatooModuleDirectory.dir(formatName))

      applyFormatSpecificConfiguration()
    }


    prepareModuleDescriptor = tasks.register<DokkatooPrepareModuleDescriptorTask>(
      taskNames.prepareModuleDescriptor
    ) task@{
      description = "Prepares the Dokka Module Descriptor for $formatName"
      includes.from(publication.includes)
      dokkaModuleDescriptorJson.convention(
        dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/module_descriptor.json")
      )
      moduleDirectory.set(generateModule.flatMap { it.outputDirectory })

//      dokkaSourceSets.addAllLater(providers.provider { dokkatooExtension.dokkatooSourceSets })
//      dokkaSourceSets.configureEach {
//        sourceSetScope.convention(this@task.path)
//      }
    }
  }

  private fun DokkatooGenerateTask.applyFormatSpecificConfiguration() {
    runtimeClasspath.from(
      dependencyContainers.dokkaGeneratorClasspath.map { classpath ->
        classpath.incoming.artifacts.artifactFiles
      }
    )
    generator.apply {
      publicationEnabled.convention(publication.enabled)

      failOnWarning.convention(publication.failOnWarning)
      finalizeCoroutines.convention(publication.finalizeCoroutines)
      includes.from(publication.includes)
      moduleName.convention(publication.moduleName)
      moduleVersion.convention(publication.moduleVersion)
      offlineMode.convention(publication.offlineMode)
      pluginsConfiguration.addAllLater(providers.provider { publication.pluginsConfiguration })
      pluginsClasspath.from(
        dependencyContainers.dokkaPluginsIntransitiveClasspath.map { classpath ->
          classpath.incoming.artifacts.artifactFiles
        }
      )
      suppressInheritedMembers.convention(publication.suppressInheritedMembers)
      suppressObviousFunctions.convention(publication.suppressObviousFunctions)
    }
  }
}
