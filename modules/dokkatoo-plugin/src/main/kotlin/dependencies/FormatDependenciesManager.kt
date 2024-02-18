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

  private fun AttributeContainer.jvmJar() {
    attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
    attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
    attribute(BUNDLING_ATTRIBUTE, objects.named(EXTERNAL))
    attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(STANDARD_JVM))
    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
  }

//  /** Collect [BaseDependencyManager.declaredDependencies]. */
//  val dokkatooResolver: NamedDomainObjectProvider<Configuration> =
//    project.configurations.register(configurationNames.dokkatooResolver) {
//      description = "Resolve Dokkatoo declared dependencies for $formatName."
//      resolvable()
//      extendsFrom(baseDependencyManager.declaredDependencies)
//      attributes {
//        attribute(USAGE_ATTRIBUTE, baseAttributes.dokkatooUsage)
//        attribute(DokkatooFormatAttribute, formatAttributes.format)
//      }
//    }


  //region Dokka Generator Plugins
  /**
   * Dokka plugins.
   *
   * Users can add plugins to this dependency.
   *
   * Should not contain runtime dependencies - use [dokkaGeneratorClasspath].
   */
  private val dokkaPlugins: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.plugins) {
      description = "Dokka Plugins classpath for $formatName."
      declarable()
    }

  private val dokkaModulePlugins: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.modulePlugins) {
      description = "Dokka Plugins classpath for generating $formatName Modules."
      declarable()
      extendsFrom(dokkaPlugins.get())
    }

    val dokkaPublicationPlugins: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.publicationPlugins) {
      description = "Dokka Plugins classpath for generating a $formatName Publication."
      declarable()
      extendsFrom(dokkaPlugins.get())
    }

  val dokkaModulePluginsResolver: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.modulePluginsResolver) {
      description = "Resolves Dokka Plugins classpath for $formatName Modules"
      resolvable()
      extendsFrom(dokkaModulePlugins.get())
      attributes {
        // Don't declare any other attributes, Gradle is buggy, and the devs can't fix it https://github.com/gradle/gradle/issues/18846,
        jvmJar()
      }
    }

  val dokkaModulePluginsIntransitiveClasspathResolver: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.modulePluginsIntransitiveClasspathResolver) {
      description =
        "Resolves Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
      resolvable()
      extendsFrom(dokkaModulePlugins.get())
      isTransitive = false
      attributes {
        // Don't declare any other attributes, Gradle is buggy, and the devs can't fix it https://github.com/gradle/gradle/issues/18846,
        jvmJar()
      }
    }

  val dokkaPublicationPluginsResolver: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.publicationPluginsResolver) {
      description =
        "Resolves Dokka Plugins classpath for $formatName - for internal use."
      resolvable()
      extendsFrom(dokkaPublicationPlugins.get())
      attributes {
        // Don't declare any other attributes, Gradle is buggy, and the devs can't fix it https://github.com/gradle/gradle/issues/18846,
        jvmJar()
      }
    }

  val dokkaPublicationPluginsIntransitiveResolver: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.publicationPluginsIntransitiveResolver) {
      description =
        "Resolves Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
      resolvable()
      extendsFrom(dokkaPublicationPlugins.get())
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
//  val dokkaPluginsIntransitiveClasspathResolver: NamedDomainObjectProvider<Configuration> =
//    project.configurations.register(configurationNames.pluginsClasspathIntransitiveResolver) {
//      description =
//        "Resolves Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
//      resolvable()
//      extendsFrom(dokkaPluginsClasspath.get())
//      isTransitive = false
//      attributes {
//        jvmJar()
//        attribute(DokkatooFormatAttribute, formatAttributes.format)
//        attribute(DokkatooClasspathAttribute, baseAttributes.dokkaPlugins)
//      }
//    }
  //endregion

  //region Dokka Plugins for Publication Generation
//  private val dokkaPublicationPluginClasspath: NamedDomainObjectProvider<Configuration> =
//    project.configurations.register(configurationNames.publicationPluginClasspath) {
//      description =
//        "Dokka Plugins classpath for a $formatName Publication (consisting of 1+ Dokka Module)."
//      declarable()
//      extendsFrom(baseDependencyManager.declaredDependencies)
//    }

//  val dokkaPublicationPluginClasspathResolver: NamedDomainObjectProvider<Configuration> =
//    project.configurations.register(configurationNames.publicationPluginClasspathResolver) {
//      description =
//        "Resolves Dokka Plugins classpath for a $formatName Publication (consisting of 1+ Dokka Module)."
//      resolvable()
//      extendsFrom(dokkaPublicationPluginClasspath.get())
//      attributes {
//        jvmJar()
//        attribute(DokkatooFormatAttribute, formatAttributes.format)
//        attribute(DokkatooClasspathAttribute, baseAttributes.dokkaPublicationPlugins)
//      }
//    }

//  val dokkaPublicationPluginClasspathApiOnly: NamedDomainObjectProvider<Configuration> =
//    project.configurations.register(configurationNames.publicationPluginClasspathApiOnly) {
//      description =
//        "Dokka Plugins for consumers that will assemble a $formatName Publication using the Dokka Module that this project produces"
//      declarable()
//    }

//  init {
//    project.configurations.register(configurationNames.publicationPluginClasspathApiOnlyConsumable) {
//      description =
//        "Shared Dokka Plugins for consumers that will assemble a $formatName Publication using the Dokka Module that this project produces"
//      consumable()
//      extendsFrom(dokkaPublicationPluginClasspathApiOnly.get())
//      attributes {
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
  private val dokkaGeneratorClasspath: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.generatorClasspath) {
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
  private val dokkaModuleGeneratorClasspath: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.moduleGeneratorClasspath) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      declarable()
      extendsFrom(dokkaGeneratorClasspath.get())
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
  val dokkaModuleGeneratorClasspathResolver: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.moduleGeneratorClasspathResolver) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      resolvable()

      // extend from plugins classpath, so Dokka Worker can run the plugins
      extendsFrom(dokkaModuleGeneratorClasspath.get())
      // extend from plugins classpath, so Dokka Worker can run the plugins
      extendsFrom(dokkaModulePlugins.get())

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
  private val dokkaPublicationGeneratorClasspath: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.publicationGeneratorClasspath) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      declarable()
      extendsFrom(dokkaGeneratorClasspath.get())
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
  val dokkaPublicationGeneratorClasspathResolver: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.publicationGeneratorClasspathResolver) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      resolvable()

      // extend from plugins classpath, so Dokka Worker can run the plugins
      extendsFrom(dokkaPublicationGeneratorClasspath.get())
      extendsFrom(dokkaPublicationPlugins.get())

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


  val dokkatooModuleComponentsResolver =
    project.configurations.register(configurationNames.moduleComponentsResolver) {
      resolvable()
      extendsFrom(baseDependencyManager.declaredDependencies)
      attributes {
        attribute(DokkatooFormatAttribute, formatAttributes.format)
        attribute(DokkatooComponentAttribute, baseAttributes.dokkaModuleComponents)
      }
    }

  val dokkatooModuleComponentsConsumable =
    project.configurations.register(configurationNames.moduleComponentsConsumable) {
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
