package dev.adamko.dokkatoo.distributions

import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKATOO_COMPONENT_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKATOO_FORMAT_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkatooComponentType
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkatooComponentType.GeneratorClasspath
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkatooComponentType.PluginsClasspath
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.PublicationFormatAttribute
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.asConsumer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.EXTERNAL
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.*


/**
 * The Dokka-specific Gradle [Configuration]s used to produce and consume files from external sources
 * (example: Maven Central), or between subprojects.
 *
 * (Be careful of the confusing names: Gradle [Configuration]s are used to transfer files,
 * [DokkaConfiguration][org.jetbrains.dokka.DokkaConfiguration]
 * is used to configure Dokka behaviour.)
 */
@DokkatooInternalApi
class FormatDependencyContainers(
  private val formatName: PublicationFormatAttribute,
//  dokkatooConsumer: NamedDomainObjectProvider<Configuration>,
  private val objects: ObjectFactory,
  configurations: ConfigurationContainer,
) {

  private val dependencyContainerNames = DependencyContainerNames(formatName.name)

  private val dokkatooAttributes: DokkatooConfigurationAttributes = objects.newInstance()

  private fun AttributeContainer.dokkatooComponent(
    component: DokkatooComponentType
  ) {
    attribute(DOKKATOO_COMPONENT_ATTRIBUTE, component)
    attribute(DOKKATOO_FORMAT_ATTRIBUTE, formatName)
  }

  private fun AttributeContainer.jvmJar() {
    attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
    attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
    attribute(BUNDLING_ATTRIBUTE, objects.named(EXTERNAL))
    attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(STANDARD_JVM))
    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
  }

//    //<editor-fold desc="Dokka Parameters JSON files">
//    // TODO sharing parameters is required for a 'DokkaCollect' equivalent, but this is not implemented yet
//    /** Fetch Dokka Parameter files from other subprojects */
//    @Suppress("unused")
//    @Deprecated("Not used")
//    val dokkaParametersConsumer: NamedDomainObjectProvider<Configuration> =
//      project.configurations.register(dependencyContainerNames.dokkatooParametersConsumer) {
//        description = "Fetch Dokka Parameters for $formatName from other subprojects"
//        asConsumer()
//        extendsFrom(dokkatooConsumer.get())
//        attributes {
//          dokkaCategory(dokkatooAttributes.dokkaParameters)
//        }
//      }
//
//    /** Provide Dokka Parameter files to other subprojects */
//    @Suppress("unused")
//    @Deprecated("Not used")
//    val dokkaParametersOutgoing: NamedDomainObjectProvider<Configuration> =
//      project.configurations.register(dependencyContainerNames.dokkatooParametersOutgoing) {
//        description = "Provide Dokka Parameters for $formatName to other subprojects"
//        asProvider()
//        // extend from dokkaConfigurationsConsumer, so Dokka Module Configs propagate api() style
//        //extendsFrom(dokkaParametersConsumer.get())
//        attributes {
//          dokkaCategory(dokkatooAttributes.dokkaParameters)
//        }
//      }
//    //</editor-fold>

//    //<editor-fold desc="Dokka Module files">
//    /** Fetch Dokka Module files from other subprojects */
//    val dokkaModuleConsumer: NamedDomainObjectProvider<Configuration> =
//      project.configurations.register(dependencyContainerNames.dokkatooModuleFilesConsumer) {
//        description = "Fetch Dokka Module files for $formatName from other subprojects"
//        asConsumer()
//        extendsFrom(dokkatooConsumer.get())
//        attributes {
//          dokkaCategory(dokkatooAttributes.dokkaModuleFiles)
//        }
//      }
//    /** Provide Dokka Module files to other subprojects */
//    val dokkaModuleOutgoing: NamedDomainObjectProvider<Configuration> =
//      project.configurations.register(dependencyContainerNames.dokkatooModuleFilesProvider) {
//        description = "Provide Dokka Module files for $formatName to other subprojects"
//        asProvider()
//        // extend from dokkaConfigurationsConsumer, so Dokka Module Configs propagate api() style
//        extendsFrom(dokkaModuleConsumer.get())
//        attributes {
//          dokkaCategory(dokkatooAttributes.dokkaModuleFiles)
//        }
//      }
//    //</editor-fold>

  //<editor-fold desc="Dokka Generator Plugins">
  /**
   * Dokka plugins.
   *
   * Users can add plugins to this dependency.
   *
   * Should not contain runtime dependencies.
   */
  val dokkaPluginsClasspath: NamedDomainObjectProvider<Configuration> =
    configurations.register(dependencyContainerNames.dokkaPluginsClasspath) {
      description = "Dokka Plugins classpath for $formatName"
      asConsumer()
      attributes {
        jvmJar()
        dokkatooComponent(PluginsClasspath)
      }
    }

  /**
   * Dokka Plugins, without transitive dependencies.
   *
   * It extends [dokkaPluginsClasspath], so do not add dependencies to this configuration -
   * the dependencies are computed automatically.
   */
  val dokkaPluginsIntransitiveClasspath: NamedDomainObjectProvider<Configuration> =
    configurations.register(dependencyContainerNames.dokkaPluginsIntransitiveClasspath) {
      description =
        "Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
      asConsumer()
      extendsFrom(dokkaPluginsClasspath.get())
      isTransitive = false
      attributes {
        jvmJar()
        dokkatooComponent(PluginsClasspath)
      }
    }
  //</editor-fold>

  //<editor-fold desc="Dokka Generator Classpath">
  /**
   * Runtime classpath used to execute Dokka Worker.
   *
   * This configuration is not exposed to other subprojects.
   *
   * Extends [dokkaPluginsClasspath].
   *
   * @see dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
   * @see dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
   */
  val dokkaGeneratorClasspath: NamedDomainObjectProvider<Configuration> =
    configurations.register(dependencyContainerNames.dokkaGeneratorClasspath) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      asConsumer()

      // extend from plugins classpath, so Dokka Worker can run the plugins
      extendsFrom(dokkaPluginsClasspath.get())

      isTransitive = true
      attributes {
        jvmJar()
        dokkatooComponent(GeneratorClasspath)
      }
    }
  //</editor-fold>
}
