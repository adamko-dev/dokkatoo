package dev.adamko.dokkatoo.distributions

import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKATOO_COMPONENT_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkatooComponentType.ModuleFiles
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkatooComponentType.SourceSetFiles
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.asConsumer
import dev.adamko.dokkatoo.internal.asProvider
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration


/**
 * The Dokka-specific Gradle [Configuration]s used to produce and consume files from external sources
 * (example: Maven Central), or between subprojects.
 *
 * These configurations are not for a specific Dokka Format.
 *
 * (Be careful of the confusing names: Gradle [Configuration]s are used to transfer files,
 * [DokkaConfiguration][dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec]
 * is used to configure Dokka behaviour.)
 */
@DokkatooInternalApi
class BaseDependencyContainers(
  project: Project,
) {

  private val names = DependencyContainerNames(null)

  val dokkatooModuleConsumer: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(names.dokkatooModule) {
      description =
        "Consume Dokkatoo Modules from other subprojects, to be aggregated into a modular Dokkatoo Publication"
      asConsumer()
      attributes {
        attribute(DOKKATOO_COMPONENT_ATTRIBUTE, ModuleFiles)
      }

      resolutionStrategy {
        componentSelection {
          all {
            println("doing component selection for ${this.candidate}")
            println("doing component selection for ${this.metadata?.attributes}")

            val att = metadata?.attributes?.getAttribute(DOKKATOO_COMPONENT_ATTRIBUTE)

            if (att != ModuleFiles) {
              println("rejecting $att")
              reject("no module files")
            } else {
              println("approving $att")
            }
          }
        }
      }
    }

//  val dokkatooModuleProviderFiles: NamedDomainObjectProvider<Configuration> =
//    project.configurations.register(names.dokkatooModuleElements) {
//      description = "Provide Dokkatoo Modules to other subprojects"
//      asProvider()
//      attributes {
//        attribute(DOKKATOO_COMPONENT_ATTRIBUTE, ModuleFiles)
//      }
//    }

  val dokkatooSourceSetsConsumer: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(names.dokkatooSourceSets) {
      description =
        "Consume Dokkatoo Source Sets from other subprojects, to be merged into a non-modular Dokkatoo Publication"
      asConsumer()
      attributes {
        attribute(DOKKATOO_COMPONENT_ATTRIBUTE, SourceSetFiles)
      }

      resolutionStrategy {
        componentSelection {
          all {
            if (metadata?.attributes?.getAttribute(DOKKATOO_COMPONENT_ATTRIBUTE) != SourceSetFiles) {
              reject("no source set files")
            }
          }
        }
      }
    }

//  val dokkatooSourceSetFilesProvider: NamedDomainObjectProvider<Configuration> =
//    project.configurations.register(names.dokkatooSourceSetsElements) {
//      description = "Provide Dokkatoo Source Sets to other subprojects"
//      asProvider()
//      attributes {
//        attribute(DOKKATOO_COMPONENT_ATTRIBUTE, SourceSetFiles)
//      }
//    }

  val dokkatooComponentsProvider: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(names.dokkatooSourceSetsElements) {
      description = "Provide Dokkatoo components to other subprojects"
      asProvider()
//      attributes {
//        attribute(DOKKATOO_COMPONENT_ATTRIBUTE, SourceSetFiles)
//      }
    }
}
