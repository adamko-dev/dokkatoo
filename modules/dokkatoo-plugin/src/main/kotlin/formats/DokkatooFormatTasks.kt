package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.LocalProjectOnlyFilter
import dev.adamko.dokkatoo.internal.configuring
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import dev.adamko.dokkatoo.tasks.DokkatooPrepareModuleDescriptorTask
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*

/** Tasks for generating a Dokkatoo Publication in a specific format. */
@DokkatooInternalApi
class DokkatooFormatTasks(
  project: Project,
  private val publication: DokkaPublication,
  private val dokkatooExtension: DokkatooExtension,
  private val dependencyContainers: DokkatooFormatDependencyContainers,

  private val providers: ProviderFactory,
) {
  private val formatName: String get() = publication.formatName

  private val taskNames = DokkatooBasePlugin.TaskNames(formatName)

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

  val generatePublication = project.tasks.register<DokkatooGenerateTask>(
    taskNames.generatePublication,
    publication.pluginsConfiguration,
  ).configuring task@{
    description = "Executes the Dokka Generator, generating the $formatName publication"
    generationType.set(DokkatooGenerateTask.GenerationType.PUBLICATION)
    rootDirectory.set(project.rootProject.rootDir.absolutePath)

    outputDirectory.convention(dokkatooExtension.dokkatooPublicationDirectory.dir(formatName))

    generator.apply {
      // depend on Dokka Module Descriptors from other subprojects
      dokkaModuleFiles.from(
        dependencyContainers.dokkaModuleConsumer.map { modules ->
          modules.incoming
            .artifactView { componentFilter(LocalProjectOnlyFilter) }
            .artifacts.artifactFiles
        }
      )
    }

    applyFormatSpecificConfiguration()
  }

  val generateModule = project.tasks.register<DokkatooGenerateTask>(
    taskNames.generateModule,
    publication.pluginsConfiguration,
  ).configuring task@{
    description = "Executes the Dokka Generator, generating a $formatName module"
    generationType.set(DokkatooGenerateTask.GenerationType.MODULE)
    rootDirectory.set(project.rootProject.rootDir.absolutePath)

    outputDirectory.convention(dokkatooExtension.dokkatooModuleDirectory.dir(formatName))

    applyFormatSpecificConfiguration()
  }

  val prepareModuleDescriptor = project.tasks.register<DokkatooPrepareModuleDescriptorTask>(
    taskNames.prepareModuleDescriptor
  ) task@{
    description = "Prepares the Dokka Module Descriptor for $formatName"
    val rootDirectoryFile = project.rootProject.rootDir
    rootDirectory.set(rootDirectoryFile.absolutePath)

    includes.addAll(publication.includes.map { it.relativeTo(rootDirectoryFile).path })
    dokkaModuleDescriptorJson.convention(
      dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/module_descriptor.json")
    )
    moduleDirectory.set(generateModule.flatMap { it.outputDirectory.asFile.map { it.relativeTo(rootDirectoryFile).path } })

//      dokkaSourceSets.addAllLater(providers.provider { dokkatooExtension.dokkatooSourceSets })
//      dokkaSourceSets.configureEach {
//        sourceSetScope.convention(this@task.path)
//      }
  }
}
