package dev.adamko.dokkatoo.dependencies

import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooClasspathAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooFormatAttribute
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
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
 */
/**
 * The Dokka-specific Gradle [Configuration]s used to declare, resolve, and share dependencies
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

  /** Collect [BaseDependencyManager.declaredDependencies]. */
  val incoming: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.dokkatooResolver) {
      description = "Resolve Dokkatoo declared dependencies for $formatName."
      resolvable()
      extendsFrom(baseDependencyManager.declaredDependencies)
      attributes {
        attribute(USAGE_ATTRIBUTE, baseAttributes.dokkatooUsage)
        attribute(DokkatooFormatAttribute, formatAttributes.format)
      }
    }


  //region Dokka Generator Plugins
  /**
   * Dokka plugins.
   *
   * Users can add plugins to this dependency.
   *
   * Should not contain runtime dependencies - use [dokkaGeneratorClasspath].
   */
  val dokkaPluginsClasspath: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.pluginsClasspath) {
      description = "Dokka Plugins classpath for $formatName."
      declarable()
    }

  /**
   * Resolves Dokka Plugins, without transitive dependencies.
   *
   * It extends [dokkaPluginsClasspath].
   */
  val dokkaPluginsIntransitiveClasspathResolver: Configuration =
    project.configurations.create(configurationNames.pluginsClasspathIntransitiveResolver) {
      description =
        "Resolves Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
      resolvable()
      extendsFrom(dokkaPluginsClasspath.get())
      isTransitive = false
      attributes {
        jvmJar()
        attribute(DokkatooFormatAttribute, formatAttributes.format)
        attribute(DokkatooClasspathAttribute, baseAttributes.dokkaPlugins)
      }
    }
  //endregion

  //region Dokka Generator Classpath
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
  private val dokkaGeneratorClasspath: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.generatorClasspath) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      declarable()

      // extend from plugins classpath, so Dokka Worker can run the plugins
      extendsFrom(dokkaPluginsClasspath.get())
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
  val dokkaGeneratorClasspathResolver: Configuration =
    project.configurations.create(configurationNames.generatorClasspathResolver) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      resolvable()

      // extend from plugins classpath, so Dokka Worker can run the plugins
      extendsFrom(dokkaGeneratorClasspath.get())

      attributes {
        jvmJar()
        attribute(DokkatooFormatAttribute, formatAttributes.format)
        attribute(DokkatooClasspathAttribute, baseAttributes.dokkaGenerator)
      }
    }
  //endregion

  private fun componentDependencies(
    component: DokkatooAttribute.ModuleComponent
  ): ModuleComponentDependencies =
    ModuleComponentDependencies(
      project = project,
      component = component,
      baseAttributes = baseAttributes,
      formatAttributes = formatAttributes,
      declaredDependencies = baseDependencyManager.declaredDependencies,
      baseConfigurationName = configurationNames.dokkatoo,
    )

  /**
   * Output directories of a Dokka Module.
   *
   * Contains
   *
   * - `module-descriptor.json`
   * - module output directory
   * - module includes directory
   *
   * @see dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
   * @see org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory
   */
  val moduleOutputDirectories: ModuleComponentDependencies =
    componentDependencies(formatAttributes.moduleOutputDirectories)
}
