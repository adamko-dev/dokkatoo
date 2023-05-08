package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.distibutions.DokkatooDependencyContainers
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.LocalProjectOnlyFilter
import dev.adamko.dokkatoo.internal.configuring
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*


@DokkatooInternalApi
class DokkatooTasksContainer(
  project: Project,
  private val publication: DokkaPublication,
  private val dokkatooExtension: DokkatooExtension,
  private val dependencyContainers: DokkatooDependencyContainers,

  private val providers: ProviderFactory,
) {
  private val formatName: String get() = publication.formatName

  private val taskNames = DokkatooTaskNames(formatName)

  val prepareParameters = project.tasks.register<DokkatooPrepareParametersTask>(
    taskNames.prepareParameters,
    publication.pluginsConfiguration,
  ).configuring task@{
    description =
      "Prepares Dokka parameters for generating the $formatName publication"

    dokkaConfigurationJson.convention(
      dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/dokka_parameters.json")
    )

    // depend on Dokka Module Descriptors from other subprojects
    dokkaModuleFiles.from(
      dependencyContainers.dokkaModuleConsumer.map { elements ->
        elements.incoming
          .artifactView { componentFilter(LocalProjectOnlyFilter) }
          .artifacts.artifactFiles
      }
    )

    publicationEnabled.convention(publication.enabled)

//    cacheRoot.convention(publication.cacheRoot)
    delayTemplateSubstitution.convention(publication.delayTemplateSubstitution)
    failOnWarning.convention(publication.failOnWarning)
    finalizeCoroutines.convention(publication.finalizeCoroutines)
//    includes.from(publication.includes)
    moduleName.convention(publication.moduleName)
    moduleVersion.convention(publication.moduleVersion)
    offlineMode.convention(publication.offlineMode)

    pluginsConfiguration.addAllLater(providers.provider { publication.pluginsConfiguration })

    suppressInheritedMembers.convention(publication.suppressInheritedMembers)
    suppressObviousFunctions.convention(publication.suppressObviousFunctions)
  }

  val generatePublication = project.tasks.register<DokkatooGenerateTask>(
    taskNames.generatePublication
  ) task@{
    description = "Executes the Dokka Generator, generating the $formatName publication"
    generationType.set(DokkatooGenerateTask.GenerationType.PUBLICATION)

    outputDirectory.convention(dokkatooExtension.dokkatooPublicationDirectory.dir(formatName))
    dokkaParametersJson.convention(prepareParameters.flatMap { it.dokkaConfigurationJson })
    runtimeClasspath.from(
      dependencyContainers.dokkaGeneratorClasspath.map { classpath ->
        classpath.incoming.artifacts.artifactFiles
      }
    )
    dokkaModuleFiles.from(
      dependencyContainers.dokkaModuleConsumer.map { modules ->
        modules.incoming
          .artifactView { componentFilter(LocalProjectOnlyFilter) }
          .artifacts.artifactFiles
      }
    )
    pluginsClasspath.from(
      dependencyContainers.dokkaPluginsIntransitiveClasspath.map { classpath ->
        classpath.incoming.artifacts.artifactFiles
      }
    )
  }

  val generateModule = project.tasks.register<DokkatooGenerateTask>(
    taskNames.generateModule
  ) task@{
    description = "Executes the Dokka Generator, generating a $formatName module"
    generationType.set(DokkatooGenerateTask.GenerationType.MODULE)

    outputDirectory.convention(dokkatooExtension.dokkatooModuleDirectory.dir(formatName))
    dokkaParametersJson.convention(prepareParameters.flatMap { it.dokkaConfigurationJson })
    runtimeClasspath.from(
      dependencyContainers.dokkaGeneratorClasspath.map { classpath ->
        classpath.incoming.artifacts.artifactFiles
      }
    )
    pluginsClasspath.from(
      dependencyContainers.dokkaPluginsIntransitiveClasspath.map { classpath ->
        classpath.incoming.artifacts.artifactFiles
      }
    )
  }

  val prepareModuleDescriptor = project.tasks.register<DokkatooPrepareModuleDescriptorTask>(
    taskNames.prepareModuleDescriptor
  ) task@{
    description = "Prepares the Dokka Module Descriptor for $formatName"
    //includes.from(publication.includes)
    dokkaModuleDescriptorJson.convention(
      dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/module_descriptor.json")
    )
    //moduleDirectory.set(generateModule.flatMap { it.outputDirectory })

//      dokkaSourceSets.addAllLater(providers.provider { dokkatooExtension.dokkatooSourceSets })
//      dokkaSourceSets.configureEach {
//        sourceSetScope.convention(this@task.path)
//      }
  }
}
