package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_BASE_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_CATEGORY_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKA_FORMAT_ATTRIBUTE
import dev.adamko.dokkatoo.dokka.parameters.DokkaPluginConfigurationSpec.EncodedFormat
import dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier
import dev.adamko.dokkatoo.formats.*
import dev.adamko.dokkatoo.internal.*
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import dev.adamko.dokkatoo.tasks.DokkatooPrepareModuleDescriptorTask
import dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask
import dev.adamko.dokkatoo.tasks.DokkatooTask
import java.net.URL
import javax.inject.Inject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.dokka.Platform

/**
 * The base plugin for Dokkatoo. Sets up Dokkatoo and configures default values, but does not
 * add any specific config (specifically, it does not create Dokka Publications).
 */
abstract class DokkatooBasePlugin
@DokkatooInternalApi
@Inject
constructor(
  private val providers: ProviderFactory,
  private val layout: ProjectLayout,
  private val objects: ObjectFactory,
) : Plugin<Project> {

  override fun apply(target: Project) {
    // apply the lifecycle-base plugin so the clean task is available
    target.pluginManager.apply(LifecycleBasePlugin::class)

    val dokkatooExtension = createExtension(target)

    target.tasks.createDokkaLifecycleTasks()

    val configurationAttributes = objects.newInstance<DokkatooConfigurationAttributes>()

    target.dependencies.attributesSchema {
      attribute(DOKKATOO_BASE_ATTRIBUTE)
      attribute(DOKKATOO_CATEGORY_ATTRIBUTE)
      attribute(DOKKA_FORMAT_ATTRIBUTE)
    }

    target.configurations.register(dependencyContainerNames.dokkatoo) {
      description = "Fetch all Dokkatoo files from all configurations in other subprojects"
      asConsumer()
      isVisible = false
      attributes {
        attribute(DOKKATOO_BASE_ATTRIBUTE, configurationAttributes.dokkatooBaseUsage)
      }
    }

    configureDokkaPublicationsDefaults(dokkatooExtension)
    configureDokkatooSourceSetsDefaults(dokkatooExtension)

    target.tasks.withType<DokkatooGenerateTask>().configureEach {
      cacheDirectory.convention(dokkatooExtension.dokkatooCacheDirectory)
      workerDebugEnabled.convention(false)
      workerLogFile.set(temporaryDir.resolve("dokka-worker.log"))
      // increase memory - DokkaGenerator is hungry https://github.com/Kotlin/dokka/issues/1405
      workerMinHeapSize.convention("512m")
      workerMaxHeapSize.convention("1g")
      workerJvmArgs.set(
        listOf(
          //"-XX:MaxMetaspaceSize=512m",
          "-XX:+HeapDumpOnOutOfMemoryError",
          "-XX:+AlwaysPreTouch", // https://github.com/gradle/gradle/issues/3093#issuecomment-387259298
          //"-XX:StartFlightRecording=disk=true,name={path.drop(1).map { if (it.isLetterOrDigit()) it else '-' }.joinToString("")},dumponexit=true,duration=30s",
          //"-XX:FlightRecorderOptions=repository=$baseDir/jfr,stackdepth=512",
        )
      )
    }

    target.tasks.withType<DokkatooPrepareModuleDescriptorTask>().configureEach task@{
      moduleName.convention(dokkatooExtension.moduleName)
      includes.from(providers.provider { dokkatooExtension.dokkatooSourceSets.flatMap { it.includes } })
      modulePath.convention(dokkatooExtension.modulePath)
    }

    target.tasks.withType<DokkatooPrepareParametersTask>().configureEach task@{
      onlyIf("publication must be enabled") { publicationEnabled.getOrElse(true) }
    }

    target.tasks.withType<DokkatooTask.WithSourceSets>().configureEach task@{
      addAllDokkaSourceSets(providers.provider { dokkatooExtension.dokkatooSourceSets })
      dokkaSourceSets.configureEach {
        sourceSetScope.convention(this@task.path)
      }
    }
  }

  private fun createExtension(project: Project): DokkatooExtension {
    return project.extensions.create<DokkatooExtension>(EXTENSION_NAME).apply {
      moduleName.convention(providers.provider { project.name })
      moduleVersion.convention(providers.provider { project.version.toString() })
      modulePath.convention(project.pathAsFilePath())

      sourceSetScopeDefault.convention(project.path)
      dokkatooPublicationDirectory.convention(layout.buildDirectory.dir("dokka"))
      dokkatooModuleDirectory.convention(layout.buildDirectory.dir("dokka-module"))
      dokkatooConfigurationsDirectory.convention(layout.buildDirectory.dir("dokka-config"))

      extensions.create<DokkatooExtension.Versions>("versions").apply {
        jetbrainsDokka.convention(DokkatooConstants.DOKKA_VERSION)
        jetbrainsMarkdown.convention("0.3.1")
        freemarker.convention("2.3.31")
        kotlinxHtml.convention("0.8.0")
      }
    }
  }

  /** Set defaults in all [DokkatooExtension.dokkatooPublications]s */
  private fun configureDokkaPublicationsDefaults(
    dokkatooExtension: DokkatooExtension,
  ) {
    dokkatooExtension.dokkatooPublications.all {
      enabled.convention(true)

      cacheRoot.convention(dokkatooExtension.dokkatooCacheDirectory)
      delayTemplateSubstitution.convention(false)
      failOnWarning.convention(false)
      finalizeCoroutines.convention(false)
      moduleName.convention(dokkatooExtension.moduleName)
      moduleVersion.convention(dokkatooExtension.moduleVersion)
      offlineMode.convention(false)
      outputDir.convention(dokkatooExtension.dokkatooPublicationDirectory)
      suppressInheritedMembers.convention(false)
      suppressObviousFunctions.convention(true)

      pluginsConfiguration.configureEach {
        serializationFormat.convention(EncodedFormat.JSON)
      }
    }
  }

  /** Set defaults in all [DokkatooExtension.dokkatooSourceSets]s */
  private fun configureDokkatooSourceSetsDefaults(
    dokkatooExtension: DokkatooExtension,
  ) {
    val sourceSetScopeConvention = dokkatooExtension.sourceSetScopeDefault

    dokkatooExtension.dokkatooSourceSets.all dss@{
      analysisPlatform.convention(Platform.DEFAULT)
      displayName.convention(
        analysisPlatform.map { platform ->
          name.substringBeforeLast(
            delimiter = "Main",
            missingDelimiterValue = platform.name,
          )
        }
      )
      documentedVisibilities.convention(listOf(VisibilityModifier.DEFAULT))
      jdkVersion.convention(8)
      noAndroidSdkLink.convention(true)
      noJdkLink.convention(false)
      noStdlibLink.convention(false)
      reportUndocumented.convention(false)
      skipDeprecated.convention(false)
      skipEmptyPackages.convention(true)
      sourceSetScope.convention(sourceSetScopeConvention)

      // Manually added sourceSets should not be suppressed by default. dokkatooSourceSets that are
      // automatically added by DokkatooKotlinAdapter will have a sensible value for suppress.
      suppress.convention(false)

      suppressGeneratedFiles.convention(true)

      sourceLinks.configureEach {
        localDirectory.convention(layout.projectDirectory)
        remoteLineSuffix.convention("#L")
      }

      perPackageOptions.configureEach {
        matchingRegex.convention(".*")
        suppress.convention(false)
        skipDeprecated.convention(false)
        reportUndocumented.convention(false)
      }

      externalDocumentationLinks {
        configureEach {
          packageListUrl.convention(url.map { URL(it, "package-list") })
        }

        create("jdk") {
          enabled.convention(!this@dss.noJdkLink)
          url(this@dss.jdkVersion.map { jdkVersion ->
            when {
              jdkVersion < 11 -> "https://docs.oracle.com/javase/${jdkVersion}/docs/api/"
              else            -> "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/"
            }
          })
          packageListUrl(this@dss.jdkVersion.map { jdkVersion ->
            when {
              jdkVersion < 11 -> "https://docs.oracle.com/javase/${jdkVersion}/docs/api/package-list"
              else            -> "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/element-list"
            }
          })
        }
        create("kotlinStdlib") {
          enabled.convention(!this@dss.noStdlibLink)
          url("https://kotlinlang.org/api/latest/jvm/stdlib/")
        }
        create("androidSdk") {
          enabled.convention(!this@dss.noAndroidSdkLink)
          url("https://developer.android.com/reference/kotlin/")
        }
        create("androidX") {
          enabled.convention(!this@dss.noAndroidSdkLink)
          url("https://developer.android.com/reference/kotlin/")
          packageListUrl("https://developer.android.com/reference/kotlin/androidx/package-list")
        }
      }
    }
  }

  private fun TaskContainer.createDokkaLifecycleTasks() {
    val prepareParameters = register<DokkatooTask>(taskNames.prepareParameters) {
      description = "Prepares Dokka parameters for all formats"
      dependsOn(withType<DokkatooPrepareParametersTask>())
    }

    register<DokkatooTask>(taskNames.generate) {
      description = "Generates Dokkatoo publications for all formats"
      dependsOn(prepareParameters)
      dependsOn(withType<DokkatooGenerateTask>())
    }
  }

  companion object {

    const val EXTENSION_NAME = "dokkatoo"

    /**
     * The group of all Dokkatoo Gradle tasks.
     *
     * @see org.gradle.api.Task.getGroup
     */
    const val TASK_GROUP = "dokkatoo"

    val taskNames = TaskNames(null)
    val dependencyContainerNames = DependencyContainerNames(null)

    internal val jsonMapper = Json {
      prettyPrint = true
      @OptIn(ExperimentalSerializationApi::class)
      prettyPrintIndent = "  "
    }
  }

  @DokkatooInternalApi
  abstract class HasFormatName {
    abstract val formatName: String?

    /** Appends [formatName] to the end of the string, camelcase style, if [formatName] is not null */
    protected fun String.appendFormat(): String =
      when (val name = formatName) {
        null -> this
        else -> this + name.uppercaseFirstChar()
      }
  }

  /**
   * Names of the Gradle [Configuration]s used by the [Dokkatoo Plugin][DokkatooBasePlugin].
   *
   * Beware the confusing terminology:
   * - [Gradle Configurations][org.gradle.api.artifacts.Configuration] - share files between subprojects. Each has a name.
   * - [DokkaConfiguration][org.jetbrains.dokka.DokkaConfiguration] - parameters for executing the Dokka Generator
   */
  @DokkatooInternalApi
  class DependencyContainerNames(override val formatName: String?) : HasFormatName() {

    val dokkatoo = "dokkatoo".appendFormat()

    /** Name of the [Configuration] that _consumes_ [dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs] from projects */
    val dokkatooParametersConsumer = "dokkatooParameters".appendFormat()

    /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration] to other projects */
    val dokkatooParametersOutgoing = "dokkatooParametersElements".appendFormat()

    /** Name of the [Configuration] that _consumes_ all [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] files */
    val dokkatooModuleFilesConsumer = "dokkatooModule".appendFormat()

    /** Name of the [Configuration] that _provides_ all [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] files to other projects */
    val dokkatooModuleFilesProvider = "dokkatooModuleElements".appendFormat()

    /**
     * Classpath used to execute the Dokka Generator.
     *
     * Extends [dokkaPluginsClasspath], so Dokka plugins and their dependencies are included.
     */
    val dokkaGeneratorClasspath = "dokkatooGeneratorClasspath".appendFormat()

    /** Dokka Plugins (including transitive dependencies, so this can be passed to the Dokka Generator Worker classpath) */
    val dokkaPluginsClasspath = "dokkatooPlugin".appendFormat()

    /**
     * Dokka Plugins (excluding transitive dependencies) will be used to create Dokka Generator Parameters
     *
     * Generally, this configuration should not be invoked manually. Instead, use [dokkaPluginsClasspath].
     */
    val dokkaPluginsIntransitiveClasspath = "dokkatooPluginIntransitive".appendFormat()
  }

  @DokkatooInternalApi
  class TaskNames(override val formatName: String?) : HasFormatName() {
    val generate = "dokkatooGenerate".appendFormat()
    val generatePublication = "dokkatooGeneratePublication".appendFormat()
    val generateModule = "dokkatooGenerateModule".appendFormat()
    val prepareParameters = "prepareDokkatooParameters".appendFormat()
    val prepareModuleDescriptor = "prepareDokkatooModuleDescriptor".appendFormat()
  }

}
