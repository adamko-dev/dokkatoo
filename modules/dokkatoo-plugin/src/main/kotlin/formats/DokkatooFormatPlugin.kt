package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_MODULE_FILES_CONSUMER
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_MODULE_FILES_PROVIDER
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_PARAMETERS
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_PARAMETERS_OUTGOING
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_GENERATOR_CLASSPATH
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH_OUTGOING
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.adapters.DokkatooJavaAdapter
import dev.adamko.dokkatoo.adapters.DokkatooKotlinAdapter
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_BASE_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_CATEGORY_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKA_FORMAT_ATTRIBUTE
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.dokka.parameters.DokkaPluginConfigurationSpec
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.LocalProjectOnlyFilter
import dev.adamko.dokkatoo.internal.asConsumer
import dev.adamko.dokkatoo.internal.asProvider
import dev.adamko.dokkatoo.internal.versions
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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.DokkaConfiguration

/**
 * Base Gradle Plugin for setting up a Dokka Publication for a specific format.
 *
 * [DokkatooBasePlugin] must be applied for this plugin (or any subclass) to have an effect.
 */
abstract class DokkatooFormatPlugin @Inject constructor(
  val formatName: String,
) : Plugin<Project> {

  @get:Inject
  protected abstract val objects: ObjectFactory
  @get:Inject
  protected abstract val providers: ProviderFactory


  override fun apply(target: Project) {

    // apply DokkatooBasePlugin
    target.pluginManager.apply(DokkatooBasePlugin::class)
    // apply the plugin that will autoconfigure Dokkatoo to use the sources of a Kotlin project
    target.pluginManager.apply(type = DokkatooKotlinAdapter::class)
    target.pluginManager.apply(type = DokkatooJavaAdapter::class)

    target.plugins.withType<DokkatooBasePlugin>().all {
      val dokkatooExtension = target.extensions.getByType(DokkatooExtension::class)

      val publication = dokkatooExtension.dokkatooPublications.create(formatName)

      val dokkatooConsumer = target.configurations.named(ConfigurationName.DOKKATOO)

      val dependencyCollections = DependencyCollections(
        formatName = formatName,
        dokkatooConsumer = dokkatooConsumer,
        project = target,
      )

      val dokkatooTasks = DokkatooTasks(
        project = target,
        publication = publication,
        dokkatooExtension = dokkatooExtension,
        dependencyCollections = dependencyCollections,
        objects = objects,
        providers = providers,
      )

      dependencyCollections.dokkaParametersOutgoing.configure {
        outgoing {
          artifact(dokkatooTasks.prepareParameters.flatMap { it.dokkaConfigurationJson })
        }
      }
      dependencyCollections.dokkaModuleOutgoing.configure {
        outgoing {
          artifact(dokkatooTasks.prepareModuleDescriptor.flatMap { it.dokkaModuleDescriptorJson })
        }
        outgoing {
          artifact(dokkatooTasks.generateModule.flatMap { it.outputDirectory }) {
            type = "directory"
          }
        }
      }

      // TODO DokkaCollect replacement
      //dependencyCollections.dokkaParametersOutgoing.configure {
      //  outgoing {
      //    artifact(dokkatooTasks.prepareParametersTask.flatMap { it.dokkaConfigurationJson })
      //  }
      //}

      val context = DokkatooFormatPluginContext(
        project = target,
        dokkatooExtension = dokkatooExtension,
        publication = publication,
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


  //  /**
//   * Utility for adding dependencies to [org.gradle.api.artifacts.Configuration]s.
//   */
  @DokkatooInternalApi
  class DokkatooFormatPluginContext(
    val project: Project,
    val dokkatooExtension: DokkatooExtension,
    val publication: DokkaPublication,
  ) {
    var addDefaultDokkaDependencies = true

    /** Create a [Dependency] for  */
    fun DependencyHandler.dokka(module: String): Provider<Dependency> =
      dokkatooExtension.versions.jetbrainsDokka.map { version -> create("org.jetbrains.dokka:$module:$version") }

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: Provider<Dependency>): Unit =
      addProvider(publication.configurationNames.dokkaPluginsClasspath, dependency)

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: String) {
      add(publication.configurationNames.dokkaPluginsClasspath, dependency)
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: Provider<Dependency>) {
      addProvider(publication.configurationNames.dokkaGeneratorClasspath, dependency)
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: String) {
      add(publication.configurationNames.dokkaGeneratorClasspath, dependency)
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
  class DependencyCollections(
    private val formatName: String,
    dokkatooConsumer: NamedDomainObjectProvider<Configuration>,
    project: Project,
  ) {

    private val objects: ObjectFactory = project.objects

    private fun String.appendFormatName() = this + formatName.capitalize()

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
      project.configurations.register(DOKKATOO_PARAMETERS.appendFormatName()) {
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
      project.configurations.register(DOKKATOO_PARAMETERS_OUTGOING.appendFormatName()) {
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
      project.configurations.register(DOKKATOO_MODULE_FILES_CONSUMER.appendFormatName()) {
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
      project.configurations.register(DOKKATOO_MODULE_FILES_PROVIDER.appendFormatName()) {
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
      project.configurations.register(DOKKA_PLUGINS_CLASSPATH.appendFormatName()) {
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
      project.configurations.register(DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH.appendFormatName()) {
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

    /** Provides Dokka plugins to other subprojects. */
    val dokkaPluginsClasspathOutgoing: NamedDomainObjectProvider<Configuration> =
      project.configurations.register(DOKKA_PLUGINS_CLASSPATH_OUTGOING.appendFormatName()) {
        description = "Provide the Dokka Plugins classpath for $formatName to other subprojects"
        asProvider()
        extendsFrom(dokkaPluginsClasspath.get())
        isVisible = false
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
      project.configurations.register(DOKKA_GENERATOR_CLASSPATH.appendFormatName()) {
        description =
          "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
        asConsumer()
        isVisible = false

        // also receive the classpath from other subprojects
//                extendsFrom(dokkaConsumer.get())

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
    private val dependencyCollections: DependencyCollections,

    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
  ) {
    private val formatName: String get() = publication.formatName

    private fun String.appendFormatName() = this + formatName.capitalize()


//  val taskName = object : Serializable {
//    val generatePublication = GENERATE_PUBLICATION.appendFormatName()
//    val generateModule = GENERATE_MODULE.appendFormatName()
//    val prepareParameters = PREPARE_PARAMETERS.appendFormatName()
//    val prepareModuleDescriptor = PREPARE_MODULE_DESCRIPTOR.appendFormatName()
//  }

    val prepareParameters = project.tasks.register<DokkatooPrepareParametersTask>(
      DokkatooBasePlugin.Companion.TaskName.PREPARE_PARAMETERS.appendFormatName()
    ) task@{
      description =
        "Creates Dokka Configuration for executing the Dokka Generator for the $formatName publication"

      dokkaConfigurationJson.convention(
        dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/dokka_parameters.json")
      )

      // depend on Dokka Module Descriptors from other subprojects
      dokkaModuleFiles.from(
        dependencyCollections.dokkaModuleConsumer.map { elements ->
          elements.incoming.artifactView {
            componentFilter(LocalProjectOnlyFilter)
            lenient(true)
          }.files
        }
      )

      dokkaSourceSets.addAllLater(providers.provider { dokkatooExtension.dokkatooSourceSets })

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
      pluginsClasspath.from(dependencyCollections.dokkaPluginsIntransitiveClasspath)

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
        serializationFormat.convention(DokkaConfiguration.SerializationFormat.JSON)
      }
      //</editor-fold>

      suppressInheritedMembers.convention(publication.suppressInheritedMembers)
      suppressObviousFunctions.convention(publication.suppressObviousFunctions)

      dokkaSourceSets.configureEach dss@{
        sourceSetScope.convention(this@task.path)
      }
    }

    val generatePublication = project.tasks.register<DokkatooGenerateTask>(
      DokkatooBasePlugin.Companion.TaskName.GENERATE_PUBLICATION.appendFormatName()
    ) {
      description = "Executes the Dokka Generator, generating the $formatName publication"
      generationType.set(DokkatooGenerateTask.GenerationType.PUBLICATION)

      outputDirectory.convention(dokkatooExtension.dokkatooPublicationDirectory.dir(formatName))
      dokkaParametersJson.convention(prepareParameters.flatMap { it.dokkaConfigurationJson })
      runtimeClasspath.from(dependencyCollections.dokkaGeneratorClasspath)
      dokkaModuleFiles.from(dependencyCollections.dokkaModuleConsumer)
    }

    val generateModule =
      project.tasks.register<DokkatooGenerateTask>(DokkatooBasePlugin.Companion.TaskName.GENERATE_MODULE.appendFormatName()) {
        description = "Executes the Dokka Generator, generating a $formatName module"
        generationType.set(DokkatooGenerateTask.GenerationType.MODULE)

        outputDirectory.convention(dokkatooExtension.dokkatooModuleDirectory.dir(formatName))
        dokkaParametersJson.convention(prepareParameters.flatMap { it.dokkaConfigurationJson })
        runtimeClasspath.from(dependencyCollections.dokkaGeneratorClasspath)
      }

    val prepareModuleDescriptor =
      project.tasks.register<DokkatooPrepareModuleDescriptorTask>(DokkatooBasePlugin.Companion.TaskName.PREPARE_MODULE_DESCRIPTOR.appendFormatName()) {
        description = "Prepares the Dokka Module Descriptor for $formatName"
        includes.from(publication.includes)
        dokkaModuleDescriptorJson.convention(
          dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/module_descriptor.json")
        )
        sourceOutputDirectory(generateModule.flatMap { it.outputDirectory })
      }
  }

  companion object
}
