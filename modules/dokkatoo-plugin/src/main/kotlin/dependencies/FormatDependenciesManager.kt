package dev.adamko.dokkatoo.dependencies

import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooClasspathAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooFormatAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooModuleComponentAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooModuleNameAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooModulePathAttribute
import dev.adamko.dokkatoo.internal.*
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
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
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
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


  /** Contains dependencies declared in a [Project.dependencies] block. */
  private val declaredDependencies: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.dokkatoo) {
      description = "Declared Dokkatoo dependencies for $formatName."
      declarable()
      extendsFrom(baseDependencyManager.declaredDependencies.get())
    }


  /** Collect [declaredDependencies]. */
  val incoming: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.dokkatooResolver) {
      description = "Resolve Dokkatoo declared dependencies for $formatName."
      resolvable()
      extendsFrom(declaredDependencies.get())
      attributes {
        attribute(USAGE_ATTRIBUTE, baseAttributes.dokkatooUsage)
        attribute(DokkatooFormatAttribute, formatAttributes.format)
        attributeProvider(DokkatooModulePathAttribute, baseAttributes.modulePath)
        attributeProvider(DokkatooModuleNameAttribute, baseAttributes.moduleName)
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
   * Dokka Plugins, without transitive dependencies.
   *
   * It extends [dokkaPluginsClasspath], so do not add dependencies to this configuration -
   * the dependencies are computed automatically.
   */
  val dokkaPluginsIntransitiveClasspathResolver: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.pluginsClasspathIntransitiveResolver) {
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
  val dokkaGeneratorClasspathResolver: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(configurationNames.generatorClasspathResolver) {
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


  @DokkatooInternalApi
  class ModuleComponent(
    project: Project,
    private val component: DokkatooAttribute.ModuleComponent,
    private val baseAttributes: BaseAttributes,
    private val formatAttributes: FormatAttributes,
    baseIncoming: NamedDomainObjectProvider<Configuration>,
    baseConfigurationName: String,
  ) {
    private val formatName: String get() = formatAttributes.format.name
    private val componentName: String get() = component.name

    private val resolver: NamedDomainObjectProvider<Configuration> =
      project.configurations.register("${baseConfigurationName}${componentName}Resolver") {
        description =
          "Resolves Dokkatoo $formatName $componentName files."
        resolvable()
        extendsFrom(baseIncoming.get())
        attributes {
          attribute(USAGE_ATTRIBUTE, baseAttributes.dokkatooUsage)
          attribute(DokkatooFormatAttribute, formatAttributes.format)
          attribute(DokkatooModuleComponentAttribute, component)
          attributeProvider(DokkatooModulePathAttribute, baseAttributes.modulePath)
          attributeProvider(DokkatooModuleNameAttribute, baseAttributes.moduleName)
        }
      }

    val outgoing: NamedDomainObjectProvider<Configuration> =
      project.configurations.register("${baseConfigurationName}${componentName}Consumable") {
        description =
          "Provides Dokkatoo $formatName $componentName files for consumption by other subprojects."
        consumable()
        extendsFrom(resolver.get())
        attributes {
          attribute(USAGE_ATTRIBUTE, baseAttributes.dokkatooUsage)
          attribute(DokkatooFormatAttribute, formatAttributes.format)
          attribute(DokkatooModuleComponentAttribute, component)
          attributeProvider(DokkatooModulePathAttribute, baseAttributes.modulePath)
          attributeProvider(DokkatooModuleNameAttribute, baseAttributes.moduleName)
        }
      }

    /**
     * Get all [ResolvedArtifactResult]s for this module.
     *
     * The artifacts will be filtered to ensure that
     *
     * * [DokkatooModuleComponentAttribute] equals [component]
     * * [DokkatooFormatAttribute] equals [FormatAttributes.format]
     *
     * This filtering should prevent a Gradle bug where it fetches random unrequested files.
     */
    val incomingArtifacts: Provider<List<ResolvedArtifactResult>> =
      baseIncoming.incomingArtifacts()

    private fun NamedDomainObjectProvider<Configuration>.incomingArtifacts(): Provider<List<ResolvedArtifactResult>> {
      return map { incoming ->
        incoming.incoming
          .artifactView {
            @Suppress("UnstableApiUsage")
            withVariantReselection()
            attributes {
              attribute(USAGE_ATTRIBUTE, baseAttributes.dokkatooUsage)
              attribute(DokkatooFormatAttribute, formatAttributes.format)
              attribute(DokkatooModuleComponentAttribute, component)
            }
            lenient(true)
          }
          .artifacts
          .artifacts
          .filter { artifact ->
            val variantAttributes = artifact.variant.attributes
            when {
              variantAttributes[DokkatooModuleComponentAttribute] != component      -> {
                logger.info("[${incoming.name}] ignoring artifact $artifact - DokkatooModuleComponentAttribute != $component | attributes:${variantAttributes.toMap()}")
                false
              }

              variantAttributes[DokkatooFormatAttribute] != formatAttributes.format -> {
                logger.info("[${incoming.name}] ignoring artifact $artifact - DokkatooFormatAttribute != ${formatAttributes.format} | attributes:${variantAttributes.toMap()}")
                false
              }

              else                                                                  -> {
                logger.info("[${incoming.name}] found valid artifact $artifact | attributes:${variantAttributes.toMap()}")
                true
              }
            }
          }
      }
    }

    @DokkatooInternalApi
    companion object {
      private val logger = Logging.getLogger(DokkatooAttribute.ModuleComponent::class.java)
    }
  }


  private fun ModuleComponent(component: DokkatooAttribute.ModuleComponent): ModuleComponent =
    ModuleComponent(
      project = project,
      component = component,
      baseAttributes = baseAttributes,
      formatAttributes = formatAttributes,
      baseIncoming = incoming,
      baseConfigurationName = configurationNames.dokkatoo,
    )

  /**
   * @see org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory
   */
  val moduleDirectory = ModuleComponent(formatAttributes.moduleDirectory)
  /**
   * Module includes (might not be used?)
   *
   * @see org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription.includes
   */
  val moduleIncludes = ModuleComponent(formatAttributes.moduleIncludes)
}
