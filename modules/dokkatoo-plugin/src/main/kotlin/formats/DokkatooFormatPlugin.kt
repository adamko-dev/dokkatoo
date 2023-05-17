package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.adapters.DokkatooAndroidAdapter
import dev.adamko.dokkatoo.adapters.DokkatooJavaAdapter
import dev.adamko.dokkatoo.adapters.DokkatooKotlinAdapter
import dev.adamko.dokkatoo.distributions.DependencyContainerNames
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKATOO_COMPONENT_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKATOO_FORMAT_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKA_COMPONENT_ARTIFACT_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKA_MODULE_DESCRIPTION_NAME_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkaComponentArtifactType.ModuleDescriptorJson
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkaComponentArtifactType.SourceOutputDirectory
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkatooComponentType.ModuleFiles
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkatooParametersIdAttribute.DokkaModuleDescriptionName
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.PublicationFormatAttribute
import dev.adamko.dokkatoo.distributions.FormatDependenciesManager
import dev.adamko.dokkatoo.distributions.FormatDependencyContainers
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*

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
    target.pluginManager.apply(type = DokkatooAndroidAdapter::class)

    target.plugins.withType<DokkatooBasePlugin>().configureEach {
      val dokkatooExtension = target.extensions.getByType(DokkatooExtension::class)

      val format: PublicationFormatAttribute = objects.named(formatName)
      val dependenciesManager = FormatDependenciesManager(
        objects.named(formatName),
        objects,
        providers,
        dokkatooExtension.baseDependencyContainers
      )

      val publication = dokkatooExtension.dokkatooPublications.create(format.name)

//      val dokkatooConsumer =
//        target.configurations.named(DokkatooBasePlugin.dependencyContainerNames.dokkatoo)

      val formatDependencyContainers = FormatDependencyContainers(
        formatName = format,
//        dokkatooConsumer = dokkatooConsumer,
        configurations = target.configurations,
        objects = objects
      )

      val dokkatooFormatTasks = DokkatooFormatTasks(
        tasks = target.tasks,
        publication = publication,
        dokkatooExtension = dokkatooExtension,
        dependencyContainers = formatDependencyContainers,
        providers = providers,
        dependenciesManager = dependenciesManager,
      )

      dokkatooExtension.baseDependencyContainers.dokkatooComponentsProvider.configure {
        outgoing {
          artifact(dokkatooFormatTasks.prepareModuleDescriptor.flatMap { it.dokkaModuleDescriptorJson }) {
            attributes {
              attribute(
                DOKKA_MODULE_DESCRIPTION_NAME_ATTRIBUTE,
                objects.named<DokkaModuleDescriptionName>(dokkatooExtension.moduleName.get())
              )
              attribute(DOKKATOO_COMPONENT_ATTRIBUTE, ModuleFiles)
              attribute(DOKKATOO_FORMAT_ATTRIBUTE, format)
              attribute(DOKKA_COMPONENT_ARTIFACT_ATTRIBUTE, ModuleDescriptorJson)
            }
          }
        }
      }

      val moduleName: Provider<DokkaModuleDescriptionName> =
        dokkatooExtension.moduleName.map { objects.named(it) }

      dependenciesManager.provideDokkatooModuleArtifact(
        moduleName = moduleName,
        fileType = ModuleDescriptorJson,
      ) {
        dokkatooFormatTasks.prepareModuleDescriptor.flatMap { it.dokkaModuleDescriptorJson }
      }

      dependenciesManager.provideDokkatooModuleArtifact(
        moduleName = moduleName,
        fileType = SourceOutputDirectory,
      ) {
        dokkatooFormatTasks.generateModule.flatMap { it.outputDirectory }
      }

//      formatDependencyContainers.dokkaModuleOutgoing.configure {
//        outgoing {
//          artifact(dokkatooFormatTasks.prepareModuleDescriptor.flatMap { it.dokkaModuleDescriptorJson })
//        }
//        outgoing {
//          artifact(dokkatooFormatTasks.generateModule.flatMap { it.outputDirectory }) {
//            type = "directory"
//          }
//        }
//      }

      // TODO DokkaCollect replacement - share raw files without first generating a Dokka Module
      //dependencyCollections.dokkaParametersOutgoing.configure {
      //  outgoing {
      //    artifact(dokkatooTasks.prepareParametersTask.flatMap { it.dokkaConfigurationJson })
      //  }
      //}

      val context = DokkatooFormatPluginContext(
        project = target,
        dokkatooExtension = dokkatooExtension,
        dokkatooFormatTasks = dokkatooFormatTasks,
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
    val dokkatooFormatTasks: DokkatooFormatTasks,
    formatName: String,
  ) {
    private val dependencyContainerNames = DependencyContainerNames(formatName)

    var addDefaultDokkaDependencies = true

    /** Create a [Dependency] for a Dokka module */
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
        // TODO why does org.jetbrains:markdown need a -jvm suffix?
        dokkaGenerator("org.jetbrains:markdown-jvm" version jetbrainsMarkdown)
        dokkaGenerator("org.jetbrains.kotlinx:kotlinx-coroutines-core" version kotlinxCoroutines)
      }
    }
  }
}
