package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.adapters.DokkatooJavaAdapter
import dev.adamko.dokkatoo.adapters.DokkatooKotlinAdapter
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_BASE_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_CATEGORY_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKA_FORMAT_ATTRIBUTE
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.dokka.parameters.DokkaPluginConfigurationSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaPluginConfigurationSpec.EncodedFormat
import dev.adamko.dokkatoo.internal.*
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import dev.adamko.dokkatoo.tasks.DokkatooPrepareModuleDescriptorTask
import dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
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
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.DokkaConfiguration

/**
 * Base Gradle Plugin for setting up a Dokka Publication for a specific format.
 *
 * [DokkatooBasePlugin] must be applied for this plugin (or any subclass) to have an effect.
 *
 * Anyone can use this class as a basis for a generating a Dokka Publication in a custom format.
 */
abstract class DokkatooFormatPlugin(
  val formatName: String,
) : Plugin<Project> {

  @get:Inject
  @DokkatooInternalApi
  protected abstract val objects: ObjectFactory
  @get:Inject
  @DokkatooInternalApi
  protected abstract val providers: ProviderFactory
  @get:Inject
  @DokkatooInternalApi
  protected abstract val files: FileSystemOperations


  override fun apply(target: Project) {

    // apply DokkatooBasePlugin
    target.pluginManager.apply(DokkatooBasePlugin::class)

    // apply the plugin that will autoconfigure Dokkatoo to use the sources of a Kotlin project
    target.pluginManager.apply(type = DokkatooKotlinAdapter::class)
    target.pluginManager.apply(type = DokkatooJavaAdapter::class)

    target.plugins.withType<DokkatooBasePlugin>().configureEach {
      val dokkatooExtension = target.extensions.getByType(DokkatooExtension::class)

      val publication = dokkatooExtension.dokkatooPublications.create(formatName)

      val dokkatooConsumer =
        target.configurations.named(DokkatooBasePlugin.dependencyContainerNames.dokkatoo)

      val dependencyContainers = DependencyContainers(
        formatName = formatName,
        dokkatooConsumer = dokkatooConsumer,
        project = target,
      )

      val dokkatooTasks = DokkatooTasks(
        project = target,
        publication = publication,
        dokkatooExtension = dokkatooExtension,
        dependencyContainers = dependencyContainers,
        objects = objects,
        providers = providers,
      )

      dependencyContainers.dokkaParametersOutgoing.configure {
        outgoing {
          artifact(dokkatooTasks.prepareParameters.flatMap { it.dokkaConfigurationJson })
        }
      }
      dependencyContainers.dokkaModuleOutgoing.configure {
        outgoing {
          artifact(dokkatooTasks.prepareModuleDescriptor.flatMap { it.dokkaModuleDescriptorJson })
        }
        outgoing {
          artifact(dokkatooTasks.generateModule.flatMap { it.outputDirectory }) {
            type = "directory"
          }
        }
      }

      // TODO DokkaCollect replacement - share raw files without first generating a Dokka Module
      //dependencyCollections.dokkaParametersOutgoing.configure {
      //  outgoing {
      //    artifact(dokkatooTasks.prepareParametersTask.flatMap { it.dokkaConfigurationJson })
      //  }
      //}

      val context = DokkatooFormatPluginContext(
        project = target,
        dokkatooExtension = dokkatooExtension,
        formatName = formatName,
      )

      context.configure()

      if (context.addDefaultDokkaDependencies) {
        with(context) {
          addDefaultDokkaDependencies()
        }
      }
    }
  }


  /** Format specific configuration - to be implemented by subclasses */
  open fun DokkatooFormatPluginContext.configure() {}


  @DokkatooInternalApi
  class DokkatooFormatPluginContext(
    val project: Project,
    val dokkatooExtension: DokkatooExtension,
    formatName: String,
  ) {
    private val dependencyContainerNames = DokkatooBasePlugin.DependencyContainerNames(formatName)

    var addDefaultDokkaDependencies = true

    /** Create a [Dependency] for  */
    fun DependencyHandler.dokka(module: String): Provider<Dependency> =
      dokkatooExtension.versions.jetbrainsDokka.map { version -> create("org.jetbrains.dokka:$module:$version") }

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: Provider<Dependency>): Unit =
      addProvider(dependencyContainerNames.dokkaPluginsClasspath, dependency)

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: String) {
      add(dependencyContainerNames.dokkaPluginsClasspath, dependency)
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: Provider<Dependency>) {
      addProvider(dependencyContainerNames.dokkaGeneratorClasspath, dependency)
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: String) {
      add(dependencyContainerNames.dokkaGeneratorClasspath, dependency)
    }
  }


  private fun DokkatooFormatPluginContext.addDefaultDokkaDependencies() {
    project.dependencies {
      /** lazily create a [Dependency] with the provided [version] */
      infix fun String.version(version: Property<String>): Provider<Dependency> =
        version.map { v -> create("$this:$v") }

      with(dokkatooExtension.versions) {

        dokkaPlugin(dokka("dokka-analysis"))
        dokkaPlugin(dokka("templating-plugin"))
        dokkaPlugin(dokka("dokka-base"))
        dokkaPlugin(dokka("kotlin-analysis-intellij"))
        dokkaPlugin(dokka("kotlin-analysis-compiler"))
//        dokkaPlugin(dokka("all-modules-page-plugin"))

        dokkaPlugin("org.jetbrains.kotlinx:kotlinx-html" version kotlinxHtml)
        dokkaPlugin("org.freemarker:freemarker" version freemarker)

        dokkaGenerator(dokka("dokka-core"))
        // TODO why does this need a -jvm suffix?
        dokkaGenerator("org.jetbrains:markdown-jvm" version jetbrainsMarkdown)
      }
    }
  }


  /**
   * The Dokka-specific Gradle [Configuration]s used to produce and consume files from external sources
   * (example: Maven Central), or between subprojects.
   *
   * (Be careful of the confusing names: Gradle [Configuration]s are used to transfer files,
   * [DokkaConfiguration][dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs]
   * is used to configure Dokka behaviour.)
   */
  @DokkatooInternalApi
  class DependencyContainers(
    private val formatName: String,
    dokkatooConsumer: NamedDomainObjectProvider<Configuration>,
    project: Project,
  ) {

    private val objects: ObjectFactory = project.objects

    private val dependencyContainerNames = DokkatooBasePlugin.DependencyContainerNames(formatName)

    private val dokkatooAttributes: DokkatooConfigurationAttributes = objects.newInstance()

    private fun AttributeContainer.dokkaCategory(category: DokkatooConfigurationAttributes.DokkatooCategoryAttribute) {
      attribute(DOKKATOO_BASE_ATTRIBUTE, dokkatooAttributes.dokkatooBaseUsage)
      attribute(DOKKA_FORMAT_ATTRIBUTE, objects.named(formatName))
      attribute(DOKKATOO_CATEGORY_ATTRIBUTE, category)
    }

    private fun AttributeContainer.jvmJar() {
      attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
      attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
      attribute(BUNDLING_ATTRIBUTE, objects.named(EXTERNAL))
      attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(STANDARD_JVM))
      attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
    }

    //<editor-fold desc="Dokka Parameters JSON files">
    // TODO sharing parameters is required for a 'DokkaCollect' equivalent, but this is not implemented yet
    /** Fetch Dokka Parameter files from other subprojects */
    val dokkaParametersConsumer: NamedDomainObjectProvider<Configuration> =
      project.configurations.register(dependencyContainerNames.dokkatooParametersConsumer) {
        description = "Fetch Dokka Parameters for $formatName from other subprojects"
        asConsumer()
        extendsFrom(dokkatooConsumer.get())
        isVisible = false
        attributes {
          dokkaCategory(dokkatooAttributes.dokkaParameters)
        }
      }

    /** Provide Dokka Parameter files to other subprojects */
    val dokkaParametersOutgoing: NamedDomainObjectProvider<Configuration> =
      project.configurations.register(dependencyContainerNames.dokkatooParametersOutgoing) {
        description = "Provide Dokka Parameters for $formatName to other subprojects"
        asProvider()
        // extend from dokkaConfigurationsConsumer, so Dokka Module Configs propagate api() style
        extendsFrom(dokkaParametersConsumer.get())
        isVisible = true
        attributes {
          dokkaCategory(dokkatooAttributes.dokkaParameters)
        }
      }
    //</editor-fold>

    //<editor-fold desc="Dokka Module files">
    /** Fetch Dokka Module files from other subprojects */
    val dokkaModuleConsumer: NamedDomainObjectProvider<Configuration> =
      project.configurations.register(dependencyContainerNames.dokkatooModuleFilesConsumer) {
        description = "Fetch Dokka Module files for $formatName from other subprojects"
        asConsumer()
        extendsFrom(dokkatooConsumer.get())
        isVisible = false
        attributes {
          dokkaCategory(dokkatooAttributes.dokkaModuleFiles)
        }
      }
    /** Provide Dokka Module files to other subprojects */
    val dokkaModuleOutgoing: NamedDomainObjectProvider<Configuration> =
      project.configurations.register(dependencyContainerNames.dokkatooModuleFilesProvider) {
        description = "Provide Dokka Module files for $formatName to other subprojects"
        asProvider()
        // extend from dokkaConfigurationsConsumer, so Dokka Module Configs propagate api() style
        extendsFrom(dokkaModuleConsumer.get())
        isVisible = true
        attributes {
          dokkaCategory(dokkatooAttributes.dokkaModuleFiles)
        }
      }
    //</editor-fold>

    //<editor-fold desc="Dokka Generator Plugins">
    /**
     * Dokka plugins.
     *
     * Users can add plugins to this dependency.
     *
     * Should not contain runtime dependencies.
     */
    val dokkaPluginsClasspath: NamedDomainObjectProvider<Configuration> =
      project.configurations.register(dependencyContainerNames.dokkaPluginsClasspath) {
        description = "Dokka Plugins classpath for $formatName"
        asConsumer()
        isVisible = false
        attributes {
          jvmJar()
          dokkaCategory(dokkatooAttributes.dokkaPluginsClasspath)
        }
      }

    /**
     * Dokka plugins, without transitive dependencies.
     *
     * It extends [dokkaPluginsClasspath], so do not add dependencies to this configuration -
     * the dependencies are computed automatically.
     */
    val dokkaPluginsIntransitiveClasspath: NamedDomainObjectProvider<Configuration> =
      project.configurations.register(dependencyContainerNames.dokkaPluginsIntransitiveClasspath) {
        description =
          "Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
        asConsumer()
        extendsFrom(dokkaPluginsClasspath.get())
        isVisible = false
        isTransitive = false
        attributes {
          jvmJar()
          dokkaCategory(dokkatooAttributes.dokkaPluginsClasspath)
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
      project.configurations.register(dependencyContainerNames.dokkaGeneratorClasspath) {
        description =
          "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
        asConsumer()
        isVisible = false

        // extend from plugins classpath, so Dokka Worker can run the plugins
        extendsFrom(dokkaPluginsClasspath.get())

        isTransitive = true
        attributes {
          jvmJar()
          dokkaCategory(dokkatooAttributes.dokkaGeneratorClasspath)
        }
      }
    //</editor-fold>
  }

  @DokkatooInternalApi
  class DokkatooTasks(
    project: Project,
    private val publication: DokkaPublication,
    private val dokkatooExtension: DokkatooExtension,
    private val dependencyContainers: DependencyContainers,

    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
  ) {
    private val formatName: String get() = publication.formatName

    private val taskNames = DokkatooBasePlugin.TaskNames(formatName)

    val prepareParameters = project.tasks.register<DokkatooPrepareParametersTask>(
      taskNames.prepareParameters
    ) task@{
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

      cacheRoot.convention(publication.cacheRoot)
      delayTemplateSubstitution.convention(publication.delayTemplateSubstitution)
      failOnWarning.convention(publication.failOnWarning)
      finalizeCoroutines.convention(publication.finalizeCoroutines)
      includes.from(publication.includes)
      moduleName.convention(publication.moduleName)
      moduleVersion.convention(publication.moduleVersion)
      offlineMode.convention(publication.offlineMode)
      outputDir.convention(publication.outputDir)
      pluginsClasspath.from(
        dependencyContainers.dokkaPluginsIntransitiveClasspath.map { classpath ->
          classpath.incoming.artifacts.artifactFiles
        }
      )

      pluginsConfiguration.addAllLater(providers.provider { publication.pluginsConfiguration })

      //<editor-fold desc="adapter for old DSL - to be removed">
      pluginsConfiguration.addAllLater(
        @Suppress("DEPRECATION")
        pluginsMapConfiguration.map { pluginConfig ->
          pluginConfig.map { (pluginId, pluginConfiguration) ->
            objects.newInstance<DokkaPluginConfigurationSpec>(pluginId).apply {
              values.set(pluginConfiguration)
            }
          }
        }
      )
      pluginsConfiguration.configureEach {
        serializationFormat.convention(EncodedFormat.JSON)
      }
      //</editor-fold>

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
    }

    val prepareModuleDescriptor = project.tasks.register<DokkatooPrepareModuleDescriptorTask>(
      taskNames.prepareModuleDescriptor
    ) task@{
      description = "Prepares the Dokka Module Descriptor for $formatName"
      includes.from(publication.includes)
      dokkaModuleDescriptorJson.convention(
        dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/module_descriptor.json")
      )
      moduleDirectory.set(generateModule.flatMap { it.outputDirectory })

//      dokkaSourceSets.addAllLater(providers.provider { dokkatooExtension.dokkatooSourceSets })
//      dokkaSourceSets.configureEach {
//        sourceSetScope.convention(this@task.path)
//      }
    }
  }

  @DokkatooInternalApi
  companion object
}
