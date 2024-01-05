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
import org.gradle.api.attributes.Usage
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
  formatName: String,
  baseDependencyManager: BaseDependencyManager,
  private val project: Project,
  private val objects: ObjectFactory,
) {

  private val configurationNames = DependencyContainerNames(formatName)


  //region variant attributes
  private val dokkatooUsage: Usage = baseDependencyManager.dokkatooUsage
  internal val dokkatooFormat: DokkatooAttribute.Format = objects.named(formatName)
  private val moduleName: Provider<DokkatooAttribute.ModuleName> =
    baseDependencyManager.moduleName
  private val modulePath: Provider<DokkatooAttribute.ModulePath> =
    baseDependencyManager.modulePath

  private fun AttributeContainer.jvmJar() {
    attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
    attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
    attribute(BUNDLING_ATTRIBUTE, objects.named(EXTERNAL))
    attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(STANDARD_JVM))
    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
  }
  //endregion


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
        attribute(USAGE_ATTRIBUTE, dokkatooUsage)
        attribute(DokkatooFormatAttribute, dokkatooFormat)
        attributeProvider(DokkatooModulePathAttribute, modulePath)
        attributeProvider(DokkatooModuleNameAttribute, moduleName)
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
        attribute(DokkatooFormatAttribute, dokkatooFormat)
        attribute(DokkatooClasspathAttribute, objects.named("dokka-plugins"))
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
        attribute(DokkatooFormatAttribute, dokkatooFormat)
        attribute(DokkatooClasspathAttribute, objects.named("dokka-generator"))
      }
    }
  //endregion


  @DokkatooInternalApi
  class ModuleComponent(
    project: Project,
    private val moduleAttribute: DokkatooAttribute.ModuleComponent,
    private val dokkatooUsage: Usage,
    private val moduleName: Provider<DokkatooAttribute.ModuleName>,
    private val modulePath: Provider<DokkatooAttribute.ModulePath>,
    private val dokkatooFormat: DokkatooAttribute.Format,
    baseIncoming: NamedDomainObjectProvider<Configuration>,
    baseConfigurationName: String,
  ) {
    private val componentName: String get() = moduleAttribute.name

    private val resolver: NamedDomainObjectProvider<Configuration> =
      project.configurations.register("${baseConfigurationName}${componentName}Resolver") {
        description =
          "Resolves Dokkatoo ${dokkatooFormat.name} $componentName files."
        resolvable()
        extendsFrom(baseIncoming.get())
        attributes {
          attribute(USAGE_ATTRIBUTE, dokkatooUsage)
          attribute(DokkatooFormatAttribute, dokkatooFormat)
          attribute(DokkatooModuleComponentAttribute, moduleAttribute)
          attributeProvider(DokkatooModulePathAttribute, modulePath)
          attributeProvider(DokkatooModuleNameAttribute, moduleName)
        }
      }

    val outgoing: NamedDomainObjectProvider<Configuration> =
      project.configurations.register("${baseConfigurationName}${componentName}Consumable") {
        description =
          "Provides Dokkatoo ${dokkatooFormat.name} $componentName files for consumption by other subprojects."
        consumable()
        extendsFrom(resolver.get())
        attributes {
          attribute(USAGE_ATTRIBUTE, dokkatooUsage)
          attribute(DokkatooFormatAttribute, dokkatooFormat)
          attribute(DokkatooModuleComponentAttribute, moduleAttribute)
          attributeProvider(DokkatooModulePathAttribute, modulePath)
          attributeProvider(DokkatooModuleNameAttribute, moduleName)
        }
      }

    /**
     * Get all [ResolvedArtifactResult]s for this module.
     *
     * The artifacts will be filtered to ensure that
     *
     * * [DokkatooModuleComponentAttribute] equals [moduleAttribute]
     * * [DokkatooFormatAttribute] equals [dokkatooFormat]
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
              attribute(USAGE_ATTRIBUTE, dokkatooUsage)
              attribute(DokkatooFormatAttribute, dokkatooFormat)
              attribute(DokkatooModuleComponentAttribute, moduleAttribute)
            }
            lenient(true)
          }
          .artifacts
          .artifacts
          .filter { artifact ->
            when {
              artifact.variant.attributes[DokkatooModuleComponentAttribute] != moduleAttribute -> {
                logger.info("[${incoming.name}] ignoring artifact $artifact - DokkatooModuleComponentAttribute != $moduleAttribute | attributes:${artifact.variant.attributes.toMap()}")
                false
              }

              artifact.variant.attributes[DokkatooFormatAttribute] != dokkatooFormat           -> {
                logger.info("[${incoming.name}] ignoring artifact $artifact - DokkatooFormatAttribute != $dokkatooFormat | attributes:${artifact.variant.attributes.toMap()}")
                false
              }

              else                                                                             -> {
                logger.info("[${incoming.name}] found valid artifact $artifact | attributes:${artifact.variant.attributes.toMap()}")
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


  private fun ModuleComponent(componentName: String): ModuleComponent =
    ModuleComponent(
      project = project,
      moduleAttribute = objects.named(componentName),
      dokkatooUsage = dokkatooUsage,
      moduleName = moduleName,
      modulePath = modulePath,
      dokkatooFormat = dokkatooFormat,
      baseIncoming = incoming,
      baseConfigurationName = configurationNames.dokkatoo,
    )

  /**
   * @see org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory
   */
  val moduleDirectory = ModuleComponent("ModuleDirectory")
  /**
   * Module includes (might not be used?)
   *
   * @see org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription.includes
   */
  val moduleIncludes = ModuleComponent("ModuleIncludes")
}
