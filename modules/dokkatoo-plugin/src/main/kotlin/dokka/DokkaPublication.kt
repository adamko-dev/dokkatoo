package dev.adamko.dokkatoo.dokka

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_MODULE_DESCRIPTORS_CONSUMER
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_MODULE_DESCRIPTOR_PROVIDER
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_MODULE_SOURCE_OUTPUT_CONSUMER
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_MODULE_SOURCE_OUTPUT_PROVIDER
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_PARAMETERS
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_PARAMETERS_OUTGOING
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_GENERATOR_CLASSPATH
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH_OUTGOING
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.TaskName.GENERATE_MODULE
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.TaskName.GENERATE_PUBLICATION
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.TaskName.PREPARE_MODULE_DESCRIPTOR
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.TaskName.PREPARE_PARAMETERS
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionGradleBuilder
import dev.adamko.dokkatoo.dokka.parameters.DokkaPluginConfigurationGradleBuilder
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetGradleBuilder
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*

/**
 * A [DokkaPublication] describes a single Dokka output.
 *
 * Each Publication has its own set of Gradle tasks and [org.gradle.api.artifacts.Configuration]s.
 *
 * The type of site is determined by the Dokka Plugins. By default, an HTML site will be generated.
 * By default, Dokka will create publications for HTML, Jekyll, and GitHub Flavoured Markdown.
 */
abstract class DokkaPublication @Inject constructor(
  @get:Internal
  val formatName: String,
) : Named, Serializable {

  @Internal
  override fun getName(): String = formatName

  @get:Internal
  abstract val description: Property<String>

  @get:Input
  abstract val enabled: Property<Boolean>

  @get:Input
  abstract val moduleName: Property<String>

  @get:Input
  @get:Optional
  abstract val moduleVersion: Property<String>

  @get:Internal
  // marked as Internal because this task does not use the directory contents, only the location
  abstract val outputDir: DirectoryProperty

  /**
   * Because [outputDir] must be [Internal] (so Gradle doesn't check the directory contents),
   * [outputDirPath] is required so Gradle can determine if the task is up-to-date.
   */
  @get:Input
  protected val outputDirPath: Provider<String>
    get() = outputDir.map { it.asFile.invariantSeparatorsPath }

  @get:Internal
  // marked as Internal because this task does not use the directory contents, only the location
  abstract val cacheRoot: DirectoryProperty

  /**
   * Because [cacheRoot] must be [Internal] (so Gradle doesn't check the directory contents),
   * [cacheRootPath] is required so Gradle can determine if the task is up-to-date.
   */
  @get:Input
  @get:Optional
  protected val cacheRootPath: Provider<String>
    get() = cacheRoot.map { it.asFile.invariantSeparatorsPath }

  @get:Input
  abstract val offlineMode: Property<Boolean>

  /**
   * Dokka Source Sets describe the source code that should be included in a Dokka Publication.
   *
   * Dokka will not generate documentation unless there is at least there is at least one Dokka Source Set.
   *
   * Only source sets that are contained within _this project_ should be included here.
   * To merge source sets from other projects, use the Gradle dependencies block.
   *
   * ```kotlin
   * dependencies {
   *   // merge :other-project into this project's Dokka Configuration
   *   dokka(project(":other-project"))
   * }
   * ```
   *
   * Or, to include other Dokka Publications as a Dokka Module use
   *
   * ```kotlin
   * dependencies {
   *   // include :other-project as a module in this project's Dokka Configuration
   *   dokkaModule(project(":other-project"))
   * }
   * ```
   *
   * Dokka will merge Dokka Source Sets from other subprojects.
   */
  @get:Nested
  abstract val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetGradleBuilder>

//    @get:InputFiles
//    @get:Classpath
//    abstract val pluginsClasspath: ConfigurableFileCollection

  @get:Nested
  abstract val pluginsConfiguration: NamedDomainObjectContainer<DokkaPluginConfigurationGradleBuilder>

//    /** Dokka Configuration files from other subprojects that will be merged into this Dokka Configuration */
//    @get:InputFiles
////    @get:NormalizeLineEndings
//    @get:PathSensitive(PathSensitivity.NAME_ONLY)
//    abstract val dokkaSubprojectConfigurations: ConfigurableFileCollection

//    /** Dokka Module Configuration from other subprojects. */
//    @get:InputFiles
////    @get:NormalizeLineEndings
//    @get:PathSensitive(PathSensitivity.NAME_ONLY)
//    abstract val dokkaModuleDescriptorFiles: ConfigurableFileCollection

  @get:Input
  abstract val failOnWarning: Property<Boolean>

  @get:Input
  abstract val delayTemplateSubstitution: Property<Boolean>

  @get:Input
  abstract val suppressObviousFunctions: Property<Boolean>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  abstract val includes: ConfigurableFileCollection

  @get:Input
  abstract val suppressInheritedMembers: Property<Boolean>

  @get:Input
  // TODO probably not needed any more, since Dokka Generator now runs in an isolated JVM process
  abstract val finalizeCoroutines: Property<Boolean>

  /**
   * Dokka Module Descriptions describe an independent Dokka publication, and these
   * descriptions are used by _other_ Dokka Configurations.
   *
   * The other Dokka Modules must have been generated using [delayTemplateSubstitution] set to `true`.
   *
   * Only add a module if you want the Dokka Publication produced by _this project_ to be
   * included in the Dokka Publication of _another_ project.
   */
  abstract val dokkaModules: NamedDomainObjectContainer<DokkaModuleDescriptionGradleBuilder>

  @Internal
  val taskNames = TaskNames()

  @Internal
  val configurationNames = ConfigurationNames()

  private fun String.formatSuffix() = this + formatName.capitalize()

  inner class TaskNames : Serializable {
    val generatePublication = GENERATE_PUBLICATION.formatSuffix()
    val generateModule = GENERATE_MODULE.formatSuffix()
    val prepareParameters = PREPARE_PARAMETERS.formatSuffix()
    val prepareModuleDescriptor = PREPARE_MODULE_DESCRIPTOR.formatSuffix()
  }

  // TODO rename 'outgoing' or 'provider' to 'shared'?

  inner class ConfigurationNames : Serializable {
    val dokkaParametersConsumer: String = DOKKATOO_PARAMETERS.formatSuffix()
    val dokkaParametersOutgoing: String = DOKKATOO_PARAMETERS_OUTGOING.formatSuffix()
    val moduleDescriptors = DOKKATOO_MODULE_DESCRIPTORS_CONSUMER.formatSuffix()
    val moduleDescriptorsOutgoing = DOKKATOO_MODULE_DESCRIPTOR_PROVIDER.formatSuffix()
    val moduleSourceOutputConsumer = DOKKATOO_MODULE_SOURCE_OUTPUT_CONSUMER.formatSuffix()
    val moduleSourceOutputOutgoing = DOKKATOO_MODULE_SOURCE_OUTPUT_PROVIDER.formatSuffix()
    val dokkaGeneratorClasspath = DOKKA_GENERATOR_CLASSPATH.formatSuffix()
    val dokkaPluginsClasspath = DOKKA_PLUGINS_CLASSPATH.formatSuffix()
    val dokkaPluginsIntransitiveClasspath = DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH.formatSuffix()
    val dokkaPluginsClasspathOutgoing = DOKKA_PLUGINS_CLASSPATH_OUTGOING.formatSuffix()
  }
}