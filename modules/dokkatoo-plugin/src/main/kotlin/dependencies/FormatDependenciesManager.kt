package dev.adamko.dokkatoo.dependencies

import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooComponentAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooFormatAttribute
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.consumable
import dev.adamko.dokkatoo.internal.declarable
import dev.adamko.dokkatoo.internal.resolvable
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
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
 * Dependencies for a specific Dokka Format - for example, HTML or Markdown.
 *
 * The [Configuration] here are used to declare, resolve, and share dependencies
 * from external sources (example: Maven Central), or between subprojects.
 *
 * (Be careful of the confusing names: Gradle [Configuration]s are used to transfer files,
 * [DokkaConfiguration][org.jetbrains.dokka.DokkaConfiguration]
 * is used to configure Dokka behaviour.)
 */
@DokkatooInternalApi
class FormatDependenciesManager(
  private val formatName: String,
  private val baseDependencyManager: BaseDependencyManager,
  private val project: Project,
  private val objects: ObjectFactory,
) {

  private val configurationNames = DependencyContainerNames(formatName)

  internal val baseAttributes: BaseAttributes = baseDependencyManager.baseAttributes

  internal val formatAttributes: FormatAttributes =
    FormatAttributes(
      formatName = formatName,
      objects = objects,
    )

  init {
    project.dependencies {
      applyAttributeHacks()
    }
  }

  private fun AttributeContainer.jvmJar() {
    attribute(USAGE_ATTRIBUTE, objects.named(AttributeHackPrefix + JAVA_RUNTIME))
    attribute(CATEGORY_ATTRIBUTE, objects.named(AttributeHackPrefix + LIBRARY))
    attribute(BUNDLING_ATTRIBUTE, objects.named(AttributeHackPrefix + EXTERNAL))
    attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(AttributeHackPrefix + STANDARD_JVM))
    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(AttributeHackPrefix + JAR))
  }

  //region Dokka Generator Plugins
  /**
   * Dokka plugins.
   *
   * Users can add plugins to this dependency.
   *
   * Should not contain runtime dependencies - use [dokkaGeneratorClasspath].
   */
  private val dokkaPlugins:  Configuration  =
    project.configurations.create(configurationNames.plugins) {
      description = "Dokka Plugins classpath for $formatName."
      declarable()
    }

  private val dokkaModulePlugins:  Configuration  =
    project.configurations.create(configurationNames.modulePlugins) {
      description = "Dokka Plugins classpath for generating $formatName Modules."
      declarable()
      extendsFrom(dokkaPlugins)
    }

    val dokkaPublicationPlugins:  Configuration  =
    project.configurations.create(configurationNames.publicationPlugins) {
      description = "Dokka Plugins classpath for generating a $formatName Publication."
      declarable()
      extendsFrom(dokkaPlugins)
    }

  val dokkaModulePluginsResolver:  Configuration  =
    project.configurations.create(configurationNames.modulePluginsResolver) {
      description = "Resolves Dokka Plugins classpath for $formatName Modules"
      resolvable()
      extendsFrom(dokkaModulePlugins)
      attributes {
        // Don't declare any other attributes, Gradle is buggy, and the devs can't fix it https://github.com/gradle/gradle/issues/18846,
        jvmJar()
      }
    }

  val dokkaModulePluginsIntransitiveClasspathResolver:  Configuration  =
    project.configurations.create(configurationNames.modulePluginsIntransitiveClasspathResolver) {
      description =
        "Resolves Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
      resolvable()
      extendsFrom(dokkaModulePlugins)
      isTransitive = false
      attributes {
        // Don't declare any other attributes, Gradle is buggy, and the devs can't fix it https://github.com/gradle/gradle/issues/18846,
        jvmJar()
      }
    }

  val dokkaPublicationPluginsResolver:  Configuration  =
    project.configurations.create(configurationNames.publicationPluginsResolver) {
      description =
        "Resolves Dokka Plugins classpath for $formatName - for internal use."
      resolvable()
      extendsFrom(dokkaPublicationPlugins)
      attributes {
        // Don't declare any other attributes, Gradle is buggy, and the devs can't fix it https://github.com/gradle/gradle/issues/18846,
        jvmJar()
      }
    }

  val dokkaPublicationPluginsIntransitiveResolver: Configuration =
    project.configurations.create(configurationNames.publicationPluginsIntransitiveResolver) {
      description =
        "Resolves Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
      resolvable()
      extendsFrom(dokkaPublicationPlugins)
      isTransitive = false
      attributes {
        // Don't declare any other attributes, Gradle is buggy, and the devs can't fix it https://github.com/gradle/gradle/issues/18846,
        jvmJar()
      }
    }

//  /**
//   * Resolves Dokka Plugins, without transitive dependencies.
//   *
//   * It extends [dokkaPluginsClasspath].
//   */
//  @Deprecated("split into module/publication resolvers")
//  val dokkaPluginsIntransitiveClasspathResolver:  Configuration  =
//    project.configurations.create(configurationNames.pluginsClasspathIntransitiveResolver) {
//      description =
//        "Resolves Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
//      resolvable()
//      extendsFrom(dokkaPluginsClasspath//      isTransitive = false
//      attributes {
//        jvmJar()
//        attribute(DokkatooFormatAttribute, formatAttributes.format)
//        attribute(DokkatooClasspathAttribute, baseAttributes.dokkaPlugins)
//      }
//    }
  //endregion

  //region Dokka Plugins for Publication Generation
//  private val dokkaPublicationPluginClasspath:  Configuration  =
//    project.configurations.create(configurationNames.publicationPluginClasspath) {
//      description =
//        "Dokka Plugins classpath for a $formatName Publication (consisting of 1+ Dokka Module)."
//      declarable()
//      extendsFrom(baseDependencyManager.declaredDependencies)
//    }

//  val dokkaPublicationPluginClasspathResolver:  Configuration  =
//    project.configurations.create(configurationNames.publicationPluginClasspathResolver) {
//      description =
//        "Resolves Dokka Plugins classpath for a $formatName Publication (consisting of 1+ Dokka Module)."
//      resolvable()
//      extendsFrom(dokkaPublicationPluginClasspath//      attributes {
//        jvmJar()
//        attribute(DokkatooFormatAttribute, formatAttributes.format)
//        attribute(DokkatooClasspathAttribute, baseAttributes.dokkaPublicationPlugins)
//      }
//    }

//  val dokkaPublicationPluginClasspathApiOnly:  Configuration  =
//    project.configurations.create(configurationNames.publicationPluginClasspathApiOnly) {
//      description =
//        "Dokka Plugins for consumers that will assemble a $formatName Publication using the Dokka Module that this project produces"
//      declarable()
//    }

//  init {
//    project.configurations.create(configurationNames.publicationPluginClasspathApiOnlyConsumable) {
//      description =
//        "Shared Dokka Plugins for consumers that will assemble a $formatName Publication using the Dokka Module that this project produces"
//      consumable()
//      extendsFrom(dokkaPublicationPluginClasspathApiOnly//      attributes {
//        jvmJar()
//        attribute(DokkatooFormatAttribute, formatAttributes.format)
//        attribute(DokkatooClasspathAttribute, baseAttributes.dokkaPublicationPlugins)
//      }
//    }
//  }
  //endregion

  //region Dokka Generator Classpath
  /**
   * Runtime classpath used to execute Dokka Worker.
   *
   * This configuration is not exposed to other subprojects.
   *
   * Extends [dokkaPublicationPlugins].
   *
   * @see dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
   * @see dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
   */
  private val dokkaGeneratorClasspath:  Configuration  =
    project.configurations.create(configurationNames.generatorClasspath) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      declarable()
    }

  /**
   * Runtime classpath used to execute Dokka Worker.
   *
   * This configuration is not exposed to other subprojects.
   *
   * Extends [dokkaPublicationPlugins].
   *
   * @see dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
   * @see dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
   */
  private val dokkaModuleGeneratorClasspath:  Configuration  =
    project.configurations.create(configurationNames.moduleGeneratorClasspath) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      declarable()
      extendsFrom(dokkaGeneratorClasspath)
    }

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
  val dokkaModuleGeneratorClasspathResolver:  Configuration  =
    project.configurations.create(configurationNames.moduleGeneratorClasspathResolver) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      resolvable()

      // extend from plugins classpath, so Dokka Worker can run the plugins
      extendsFrom(dokkaModuleGeneratorClasspath)
      // extend from plugins classpath, so Dokka Worker can run the plugins
      extendsFrom(dokkaModulePlugins)

      attributes {
        jvmJar()
//        attribute(DokkatooFormatAttribute, formatAttributes.format)
//        attribute(DokkatooClasspathAttribute, baseAttributes.dokkaGenerator)
      }
    }

  /**
   * Runtime classpath used to execute Dokka Worker.
   *
   * This configuration is not exposed to other subprojects.
   *
   * Extends [dokkaPublicationPlugins].
   *
   * @see dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
   * @see dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
   */
  private val dokkaPublicationGeneratorClasspath:  Configuration  =
    project.configurations.create(configurationNames.publicationGeneratorClasspath) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      declarable()
      extendsFrom(dokkaGeneratorClasspath)
    }

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
  val dokkaPublicationGeneratorClasspathResolver:  Configuration  =
    project.configurations.create(configurationNames.publicationGeneratorClasspathResolver) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      resolvable()

      // extend from plugins classpath, so Dokka Worker can run the plugins
      extendsFrom(dokkaPublicationGeneratorClasspath)
      extendsFrom(dokkaPublicationPlugins)

      attributes {
        jvmJar()
//        attribute(DokkatooFormatAttribute, formatAttributes.format)
//        attribute(DokkatooClasspathAttribute, baseAttributes.dokkaGenerator)
      }
    }
  //endregion

//  /**
//   * Output directories of a Dokka Module.
//   *
//   * Contains
//   *
//   * - `module-descriptor.json`
//   * - module output directory
//   * - module includes directory
//   *
//   * @see dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
//   * @see org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory
//   */
//  val moduleOutputDirectories: ModuleComponentDependencies =
//    componentDependencies(formatAttributes.moduleOutputDirectories)


  val dokkatooModuleComponentsResolver: Configuration =
    project.configurations.create(configurationNames.moduleComponentsResolver) {
      resolvable()
      extendsFrom(baseDependencyManager.declaredDependencies)
      attributes {
        attribute(DokkatooFormatAttribute, formatAttributes.format)
        attribute(DokkatooComponentAttribute, baseAttributes.dokkaModuleComponents)
      }
    }

  val dokkatooModuleComponentsConsumable: Configuration =
    project.configurations.create(configurationNames.moduleComponentsConsumable) {
      consumable()
      attributes {
        attribute(DokkatooFormatAttribute, formatAttributes.format)
        attribute(DokkatooComponentAttribute, baseAttributes.dokkaModuleComponents)
      }
    }

  private fun componentDependencies(
    component: DokkatooAttribute.Component,
  ): ModuleComponentDependencies =
    ModuleComponentDependencies(
      project = project,
      component = component,
      baseAttributes = baseAttributes,
      formatAttributes = formatAttributes,
      declaredDependencies = baseDependencyManager.declaredDependencies,
      baseConfigurationName = configurationNames.dokkatoo,
    )
}
