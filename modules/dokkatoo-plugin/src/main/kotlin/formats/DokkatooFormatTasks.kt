package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dependencies.FormatDependenciesManager
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.configuring
import dev.adamko.dokkatoo.tasks.*
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

/** Tasks for generating a Dokkatoo Publication in a specific format. */
@DokkatooInternalApi
class DokkatooFormatTasks(
  project: Project,
  private val publication: DokkaPublication,
  private val dokkatooExtension: DokkatooExtension,
  private val formatDependencies: FormatDependenciesManager,

  private val providers: ProviderFactory,
//  private val moduleDescriptors: NamedDomainObjectContainer<DokkaModuleDescriptionSpec>,
) {
  private val formatName: String get() = publication.formatName

  private val taskNames = TaskNames(formatName)

//  private val layout: ProjectLayout = project.layout

  private fun DokkatooGenerateTask.applyFormatSpecificConfiguration() {
    runtimeClasspath.from(
      formatDependencies.dokkaGeneratorClasspathResolver
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
        formatDependencies.dokkaPluginsIntransitiveClasspathResolver
      )
      suppressInheritedMembers.convention(publication.suppressInheritedMembers)
      suppressObviousFunctions.convention(publication.suppressObviousFunctions)
    }
  }

  val generatePublication: TaskProvider<DokkatooGeneratePublicationTask> =
    project.tasks.register<DokkatooGeneratePublicationTask>(
      taskNames.generatePublication,
      publication.pluginsConfiguration,
    ).configuring task@{
      description = "Executes the Dokka Generator, generating the $formatName publication"
//      generationType.set(DokkatooGenerateTask.GenerationType.PUBLICATION)

      outputDirectory.convention(dokkatooExtension.dokkatooPublicationDirectory.dir(formatName))

      applyFormatSpecificConfiguration()
    }

  val generateModule: TaskProvider<DokkatooGenerateModuleTask> =
    project.tasks.register<DokkatooGenerateModuleTask>(
      taskNames.generateModule,
      publication.pluginsConfiguration,
    ).configuring task@{
      description = "Executes the Dokka Generator, generating a $formatName module"
//      generationType.set(DokkatooGenerateTask.GenerationType.MODULE)

      outputDirectory.convention(dokkatooExtension.dokkatooModuleDirectory.dir(formatName))

      applyFormatSpecificConfiguration()
    }

//  private val prepareModuleDescriptor: TaskProvider<DokkatooPrepareModuleDescriptorTask> =
//    project.tasks.register<DokkatooPrepareModuleDescriptorTask>(
//      taskNames.prepareModuleDescriptor
//    ) task@{
//      description = "Prepares the Dokka Module Descriptor for $formatName"
//      moduleDescriptor.convention(temporaryDir.resolve("module-descriptor.json"))
//    }

//  val consolidateModuleElements: TaskProvider<ConsolidateDokkaModuleElementsTask> =
//    project.tasks.register<ConsolidateDokkaModuleElementsTask>(taskNames.consolidateModuleElements) {
////      outputDirectory.convention(temporaryDir)
//      outputDirectory.convention(temporaryDir)
//      moduleDescriptor.convention(prepareModuleDescriptor.flatMap { it.moduleDescriptor })
//      moduleDirectory.convention(generateModule.flatMap { it.outputDirectory })
//    }

//  //region workaround for https://github.com/gradle/gradle/issues/23708
//  private fun DirectoryProperty.convention(file: File): DirectoryProperty =
//    convention(layout.dir(providers.provider { file }))
//  private fun RegularFileProperty.convention(file: File): RegularFileProperty =
//    convention(layout.file(providers.provider { file }))
//  //endregion
}
