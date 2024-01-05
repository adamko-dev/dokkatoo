package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.adapters.DokkatooAndroidAdapter
import dev.adamko.dokkatoo.adapters.DokkatooJavaAdapter
import dev.adamko.dokkatoo.adapters.DokkatooKotlinAdapter
import dev.adamko.dokkatoo.dependencies.BaseDependencyManager
import dev.adamko.dokkatoo.dependencies.DependencyContainerNames
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooClasspathAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooFormatAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooModuleGenerateTaskPathAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooModuleNameAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooModulePathAttribute
import dev.adamko.dokkatoo.dependencies.FormatDependenciesManager
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.domainObjectContainer
import dev.adamko.dokkatoo.internal.get
import dev.adamko.dokkatoo.internal.toMap
import java.io.File
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logging
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
  @get:Inject
  @DokkatooInternalApi
  protected abstract val layout: ProjectLayout


  override fun apply(target: Project) {

    // apply DokkatooBasePlugin
    target.pluginManager.apply(DokkatooBasePlugin::class)

    // apply the plugin that will autoconfigure Dokkatoo to use the sources of a Kotlin project
    target.pluginManager.apply(type = DokkatooKotlinAdapter::class)
    target.pluginManager.apply(type = DokkatooJavaAdapter::class)
    target.pluginManager.apply(type = DokkatooAndroidAdapter::class)

    target.plugins.withType<DokkatooBasePlugin>().configureEach {
      val dokkatooExtension = target.extensions.getByType(DokkatooExtension::class)

      val publication = dokkatooExtension.dokkatooPublications.create(formatName)

      val baseDependencyManager = target.extensions.getByType<BaseDependencyManager>()

      val depsManager = FormatDependenciesManager(
        project = target,
        baseDependencyManager = baseDependencyManager,
        formatName = formatName,
        objects = objects,
      )

      val moduleDescriptors = createModuleDescriptors(depsManager)

      val dokkatooTasks = DokkatooFormatTasks(
        project = target,
        publication = publication,
        dokkatooExtension = dokkatooExtension,
        depsManager = depsManager,
        providers = providers,
        moduleDescriptors = moduleDescriptors,
      )

      depsManager
        .moduleIncludes
        .outgoing
        .configure {
          outgoing.artifacts(
            providers.provider { publication.includes }
          ) {
            builtBy(dokkatooTasks.generateModule)
            attributes {
              attributeProvider(
                // provide the full task path of the task that generates this module
                // ugly hack workaround for https://github.com/gradle/gradle/issues/13590
                DokkatooModuleGenerateTaskPathAttribute,
                dokkatooTasks.generateModule.map { objects.named(it.path) }
              )
            }
          }
          outgoing.artifacts(
            objects.fileCollection()
              .from(
                providers.provider {
                  dokkatooExtension
                    .dokkatooSourceSets
                    .map(DokkaSourceSetSpec::includes)
                }
              ).elements
              .map { it.map(FileSystemLocation::getAsFile) }
          ) {
            builtBy(dokkatooTasks.generateModule)
          }
        }

      depsManager
        .moduleDirectory
        .outgoing
        .configure {
          outgoing
            .artifact(dokkatooTasks.generateModule) {
              type = "directory"
              attributes {
                attributeProvider(
                  // provide the full task path of the task that generates this module
                  // ugly hack workaround for https://github.com/gradle/gradle/issues/13590
                  DokkatooModuleGenerateTaskPathAttribute,
                  dokkatooTasks.generateModule.map { objects.named(it.path) }
                )
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
        dokkatooTasks = dokkatooTasks,
        depsManager = depsManager,
        formatName = formatName,
      )

      context.configure()

      if (context.addDefaultDokkaDependencies) {
        with(context) {
          addDefaultDokkaDependencies()
        }
      }

      if (context.enableVersionAlignment) {
        //region version alignment
        listOf(
          depsManager.dokkaPluginsIntransitiveClasspathResolver,
          depsManager.dokkaGeneratorClasspathResolver,
        ).forEach { dependenciesContainer ->
          // Add a version if one is missing, which will allow defining a org.jetbrains.dokka
          // dependency without a version.
          // (It would be nice to do this with a virtual-platform, but Gradle is bugged:
          // https://github.com/gradle/gradle/issues/27435)
          dependenciesContainer.configure {
            resolutionStrategy.eachDependency {
              if (requested.group == "org.jetbrains.dokka" && requested.version.isNullOrBlank()) {
                logger.info("adding version of dokka dependency '$requested'")
                useVersion(dokkatooExtension.versions.jetbrainsDokka.get())
              }
            }
          }
        }
        //endregion
      }
    }
  }

  private fun createModuleDescriptors(
    depsManager: FormatDependenciesManager
  ): NamedDomainObjectContainer<DokkaModuleDescriptionSpec> {
    val incomingModuleDescriptors =
      depsManager.moduleDirectory.incomingArtifacts.map { moduleOutputDirectoryArtifact ->
        moduleOutputDirectoryArtifact.map { moduleDirArtifact ->
          createModuleDescriptor(depsManager, moduleDirArtifact)
        }
      }

    val dokkaModuleDescriptors = objects.domainObjectContainer<DokkaModuleDescriptionSpec>()
    dokkaModuleDescriptors.addAllLater(incomingModuleDescriptors)
    return dokkaModuleDescriptors
  }

  private fun createModuleDescriptor(
    depsManager: FormatDependenciesManager,
    moduleDirArtifact: ResolvedArtifactResult,
  ): DokkaModuleDescriptionSpec {
    fun missingAttributeError(name: String): Nothing =
      error("missing $name in artifact:$moduleDirArtifact, variant:${moduleDirArtifact.variant}, attributes: ${moduleDirArtifact.variant.attributes.toMap()}")

    val moduleName = moduleDirArtifact.variant.attributes[DokkatooModuleNameAttribute]
      ?: missingAttributeError("DokkatooModuleNameAttribute")

    val projectPath = moduleDirArtifact.variant.attributes[DokkatooModulePathAttribute]
      ?: missingAttributeError("DokkatooModulePathAttribute")

    val moduleGenerateTaskPath =
      moduleDirArtifact.variant.attributes[DokkatooModuleGenerateTaskPathAttribute]
        ?: missingAttributeError("DokkatooModuleGenerateTaskPathAttribute")

    val moduleDirectory = moduleDirArtifact.file

    val includes: Provider<List<File>> =
      depsManager.moduleIncludes.incomingArtifacts.map { artifacts ->
        artifacts
          .filter { artifact -> artifact.variant.attributes[DokkatooModuleNameAttribute] == moduleName }
          .map(ResolvedArtifactResult::getFile)
      }

    return objects.newInstance<DokkaModuleDescriptionSpec>(moduleName.name).apply {
      this.moduleDirectory.convention(layout.dir(providers.provider { moduleDirectory })) // https://github.com/gradle/gradle/issues/23708
      this.includes.from(includes)
      this.projectPath.convention(projectPath.name)
      this.moduleGenerateTaskPath.convention(moduleGenerateTaskPath.name)
    }
  }


  /** Format specific configuration - to be implemented by subclasses */
  open fun DokkatooFormatPluginContext.configure() {}


  @DokkatooInternalApi
  class DokkatooFormatPluginContext(
    val project: Project,
    val dokkatooExtension: DokkatooExtension,
    val dokkatooTasks: DokkatooFormatTasks,
    val depsManager: FormatDependenciesManager,
    formatName: String,
  ) {
    private val objects = project.objects
    private val dependencyContainerNames = DependencyContainerNames(formatName)

    var addDefaultDokkaDependencies = true
    var enableVersionAlignment = true

    /** Create a [Dependency] for a Dokka module */
    fun DependencyHandler.dokka(module: String): Provider<Dependency> =
      dokkatooExtension.versions.jetbrainsDokka.map { version -> create("org.jetbrains.dokka:$module:$version") }

    private fun AttributeContainer.dokkaPluginsClasspath() {
//      attribute(USAGE_ATTRIBUTE, depsManager.dokkatooUsage)
      attribute(DokkatooFormatAttribute, depsManager.dokkatooFormat)
      attribute(DokkatooClasspathAttribute, objects.named("dokka-plugins"))
    }

    private fun AttributeContainer.dokkaGeneratorClasspath() {
//      attribute(USAGE_ATTRIBUTE, depsManager.dokkatooUsage)
      attribute(DokkatooFormatAttribute, depsManager.dokkatooFormat)
      attribute(DokkatooClasspathAttribute, objects.named("dokka-generator"))
    }

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: Provider<Dependency>): Unit =
      addProvider(
        dependencyContainerNames.pluginsClasspath,
        dependency,
        Action<ExternalModuleDependency> {
          attributes { dokkaPluginsClasspath() }
        })

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: String) {
      add(dependencyContainerNames.pluginsClasspath, dependency) {
        attributes { dokkaPluginsClasspath() }
      }
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: Provider<Dependency>) {
      addProvider(dependencyContainerNames.generatorClasspath, dependency,
        Action<ExternalModuleDependency> {
          attributes { dokkaGeneratorClasspath() }
        })
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: String) {
      add(dependencyContainerNames.generatorClasspath, dependency) {
        attributes { dokkaGeneratorClasspath() }
      }
    }
  }


  private fun DokkatooFormatPluginContext.addDefaultDokkaDependencies() {
    project.dependencies {
      /** lazily create a [Dependency] with the provided [version] */
      infix fun String.version(version: Property<String>): Provider<Dependency> =
        version.map { v -> create("$this:$v") }

      with(dokkatooExtension.versions) {
        dokkaPlugin(dokka("analysis-kotlin-descriptors"))
        dokkaPlugin(dokka("templating-plugin"))
        dokkaPlugin(dokka("dokka-base"))
        //dokkaPlugin(dokka("all-modules-page-plugin"))

        dokkaPlugin("org.jetbrains.kotlinx:kotlinx-html" version kotlinxHtml)
        dokkaPlugin("org.freemarker:freemarker" version freemarker)

        dokkaGenerator(dokka("dokka-core"))
        // TODO why does org.jetbrains:markdown need a -jvm suffix?
        dokkaGenerator("org.jetbrains:markdown-jvm" version jetbrainsMarkdown)
        dokkaGenerator("org.jetbrains.kotlinx:kotlinx-coroutines-core" version kotlinxCoroutines)
      }
    }
  }

  companion object {
    private val logger = Logging.getLogger(DokkatooFormatPlugin::class.java)
  }
}
