package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_BASE_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_CATEGORY_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKA_FORMAT_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooFormatGradleConfigurations
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.dokka.parameters.DokkaPluginConfigurationSpec
import dev.adamko.dokkatoo.formats.*
import dev.adamko.dokkatoo.internal.*
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask.GenerationType.MODULE
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask.GenerationType.PUBLICATION
import dev.adamko.dokkatoo.tasks.DokkatooPrepareModuleDescriptorTask
import dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask
import dev.adamko.dokkatoo.tasks.DokkatooTask
import java.net.URL
import javax.inject.Inject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.attributes.*
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
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform

/**
 * The base plugin for Dokkatoo. Sets up Dokkatoo and configures default values, but does not
 * add any specific config (specifically, it does not create Dokka Publications).
 */
abstract class DokkatooBasePlugin @Inject constructor(
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

    val dokkaConsumerConfiguration = target.configurations.register(ConfigurationName.DOKKATOO) {
      description = "Fetch all Dokkatoo files from all configurations in other subprojects"
      asConsumer()
      isVisible = false
      attributes {
        attribute(DOKKATOO_BASE_ATTRIBUTE, configurationAttributes.dokkatooBaseUsage)
      }
    }

    configureAllDokkaPublications(
      target,
      dokkatooExtension,
      dokkaConsumerConfiguration,
      configurationAttributes,
    )

    configureDokkaPublicationsDefaults(dokkatooExtension)
    configureDokkatooSourceSetsDefaults(dokkatooExtension)

    target.tasks.withType<DokkatooGenerateTask>().configureEach {
      cacheDirectory.convention(dokkatooExtension.dokkatooCacheDirectory)
      workerDebugEnabled.convention(false)
      // increase memory - DokkaGenerator is hungry https://github.com/Kotlin/dokka/issues/1405
      workerMinHeapSize.convention("256m")
      workerMaxHeapSize.convention("1g")
      workerJvmArgs.set(listOf("-XX:MaxMetaspaceSize=512m"))
    }

    target.tasks.withType<DokkatooPrepareModuleDescriptorTask>().all task@{
      moduleName.convention(dokkatooExtension.moduleName)
      dokkatooExtension.dokkatooSourceSets.all dss@{
        this@task.includes.from(this@dss.includes)
      }
      modulePath.convention(dokkatooExtension.modulePath)
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
        jetbrainsDokka.convention("1.7.20")
        jetbrainsMarkdown.convention("0.3.1")
        freemarker.convention("2.3.31")
        kotlinxHtml.convention("0.8.0")
      }
    }
  }


  /** proactively create the tasks and configurations required for generating each Dokka Publication */
  private fun configureAllDokkaPublications(
    project: Project,
    dokkatooExtension: DokkatooExtension,
    dokkaConsumerConfiguration: NamedDomainObjectProvider<Configuration>,
    configurationAttributes: DokkatooConfigurationAttributes,
  ) {
    dokkatooExtension.dokkatooPublications.all publication@{

      // create Gradle Configurations
      val gradleConfigurations = createDokkaFormatConfigurations(
        dokkaConsumerConfiguration,
        objects,
        configurationAttributes,
        project.configurations,
      )

      // create tasks
      val prepareParametersTask = project.tasks.register<DokkatooPrepareParametersTask>(
        taskNames.prepareParameters
      ) task@{
        description =
          "Creates Dokka Configuration for executing the Dokka Generator for the $formatName publication"

        dokkaConfigurationJson.convention(
          dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/dokka_parameters.json")
        )

        // depend on Dokka Module Parameters from other subprojects
        dokkaSubprojectParameters.from(
          // TODO Merging config from other subprojects is the equivalent of the current 'Collect' task.
          //      It needs a separate Gradle Configuration so subprojects to 'Collect' can be specifically declared.
          //      Disabled for now, so I can get non-Collect functionality working.
//          gradleConfigurations.dokkaParametersConsumer.map { elements ->
//            elements.incoming.artifactView {
//              componentFilter { it is ProjectComponentIdentifier }
//              lenient(true)
//            }.files
//          }
        )

        // depend on Dokka Module Descriptors from other subprojects
        dokkaModuleFiles.from(
          gradleConfigurations.dokkaModuleConsumer.map { elements ->
            elements.incoming.artifactView {
              componentFilter(LocalProjectOnlyFilter)
              lenient(true)
            }.files
          }
        )

        dokkaSourceSets.addAllLater(providers.provider { dokkatooExtension.dokkatooSourceSets })

        publicationEnabled.convention(this@publication.enabled)
        onlyIf { publicationEnabled.getOrElse(true) }

        cacheRoot.set(this@publication.cacheRoot)
        delayTemplateSubstitution.set(this@publication.delayTemplateSubstitution)
        failOnWarning.set(this@publication.failOnWarning)
        finalizeCoroutines.set(this@publication.finalizeCoroutines)
        includes.from(this@publication.includes)
        moduleName.set(this@publication.moduleName)
        moduleVersion.set(this@publication.moduleVersion)
        offlineMode.set(this@publication.offlineMode)
        outputDir.set(this@publication.outputDir)
        pluginsClasspath.from(gradleConfigurations.dokkaPluginsIntransitiveClasspath)

        pluginsConfiguration.addAllLater(providers.provider { this@publication.pluginsConfiguration })

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

        suppressInheritedMembers.set(this@publication.suppressInheritedMembers)
        suppressObviousFunctions.set(this@publication.suppressObviousFunctions)

        dokkaSourceSets.configureEach dss@{
          sourceSetScope.convention(this@task.path)
        }
      }

      gradleConfigurations.dokkaParametersOutgoing.configure {
        outgoing {
          artifact(prepareParametersTask.flatMap { it.dokkaConfigurationJson })
        }
      }

      project.tasks.register<DokkatooGenerateTask>(taskNames.generatePublication) {
        description = "Executes the Dokka Generator, generating the $formatName publication"
        outputDirectory.convention(dokkatooExtension.dokkatooPublicationDirectory.dir(formatName))
        dokkaParametersJson.convention(prepareParametersTask.flatMap { it.dokkaConfigurationJson })
        runtimeClasspath.from(gradleConfigurations.dokkaGeneratorClasspath)
        generationType.set(PUBLICATION)
        dokkaModuleFiles.from(gradleConfigurations.dokkaModuleConsumer)
      }

      val generateModule =
        project.tasks.register<DokkatooGenerateTask>(taskNames.generateModule) {
          description = "Executes the Dokka Generator, generating a $formatName module"
          outputDirectory.convention(dokkatooExtension.dokkatooModuleDirectory.dir(formatName))
          dokkaParametersJson.convention(prepareParametersTask.flatMap { it.dokkaConfigurationJson })
          runtimeClasspath.from(gradleConfigurations.dokkaGeneratorClasspath)
          generationType.set(MODULE)
        }

      val prepareModuleDescriptorTask =
        project.tasks.register<DokkatooPrepareModuleDescriptorTask>(taskNames.prepareModuleDescriptor) {
          description = "Prepares the Dokka Module Descriptor for $formatName"
          includes.from(this@publication.includes)
          dokkaModuleDescriptorJson.convention(
            dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/module_descriptor.json")
          )
          sourceOutputDirectory(generateModule.flatMap { it.outputDirectory })
        }

      gradleConfigurations.dokkaModuleOutgoing.configure {
        outgoing {
          artifact(prepareModuleDescriptorTask.flatMap { it.dokkaModuleDescriptorJson })
        }
      }
      gradleConfigurations.dokkaModuleOutgoing.configure {
        outgoing {
          artifact(generateModule.flatMap { it.outputDirectory }) {
            type = "directory"
          }
        }
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
        serializationFormat.convention(DokkaConfiguration.SerializationFormat.JSON)
      }
    }
  }

  /** Set defaults in all [DokkatooExtension.dokkatooSourceSets]s */
  private fun configureDokkatooSourceSetsDefaults(
    dokkatooExtension: DokkatooExtension,
  ) {
    dokkatooExtension.dokkatooSourceSets.all dss@{
      analysisPlatform.convention(Platform.DEFAULT)
      displayName.convention("main")
      documentedVisibilities.convention(listOf(DokkaConfiguration.Visibility.PUBLIC))
      jdkVersion.convention(8)
      noAndroidSdkLink.convention(true)
      noJdkLink.convention(false)
      noStdlibLink.convention(false)
      reportUndocumented.convention(false)
      skipDeprecated.convention(false)
      skipEmptyPackages.convention(true)
      sourceSetScope.convention(dokkatooExtension.sourceSetScopeDefault)
      //suppress.convention(false) // TODO need to re-enable suppress convention, it's only disabled so the hack workaround 'todoSourceSetName' works
      suppressGeneratedFiles.convention(true)

      sourceLinks.configureEach {
        localDirectory.convention(layout.projectDirectory.asFile)
        remoteLineSuffix.convention("#L")
      }

      perPackageOptions.configureEach {
        matchingRegex.convention(".*")
        suppress.convention(false)
        skipDeprecated.convention(false)
        reportUndocumented.convention(false)
        @Suppress("DEPRECATION")
        includeNonPublic.convention(false)
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

  /**
   * Create [DokkatooFormatGradleConfigurations].
   *
   * (Be careful of the confusing names: Gradle [Configuration]s are used to transfer files,
   * [DokkaConfiguration][dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs]
   * is used to configure Dokka behaviour.)
   */
  private fun DokkaPublication.createDokkaFormatConfigurations(
    dokkaConsumer: NamedDomainObjectProvider<Configuration>,
    objects: ObjectFactory,
    attributes: DokkatooConfigurationAttributes,
    configurations: ConfigurationContainer,
  ): DokkatooFormatGradleConfigurations {

    fun AttributeContainer.dokkaCategory(category: DokkatooConfigurationAttributes.DokkatooCategoryAttribute) {
      attribute(DOKKATOO_BASE_ATTRIBUTE, attributes.dokkatooBaseUsage)
      attribute(DOKKA_FORMAT_ATTRIBUTE, objects.named(formatName))
      attribute(DOKKATOO_CATEGORY_ATTRIBUTE, category)
    }

    fun AttributeContainer.jvmJar() {
      attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
      attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
      attribute(BUNDLING_ATTRIBUTE, objects.named(EXTERNAL))
      attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(STANDARD_JVM))
      attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
    }

    //<editor-fold desc="Dokka Parameters JSON files">
    val dokkaParametersConsumer =
      configurations.register(configurationNames.dokkaParametersConsumer) {
        description = "Fetch Dokka Parameters for $formatName from other subprojects"
        asConsumer()
        extendsFrom(dokkaConsumer.get())
        isVisible = false
        attributes {
          dokkaCategory(attributes.dokkaParameters)
        }
      }

    val dokkaParametersOutgoing =
      configurations.register(configurationNames.dokkaParametersOutgoing) {
        description = "Provide Dokka Parameters for $formatName to other subprojects"
        asProvider()
        // extend from dokkaConfigurationsConsumer, so Dokka Module Configs propagate api() style
        extendsFrom(dokkaParametersConsumer.get())
        isVisible = true
        attributes {
          dokkaCategory(attributes.dokkaParameters)
        }
      }
    //</editor-fold>

    //<editor-fold desc="Dokka Module files">
    val dokkaModuleConsumer =
      configurations.register(configurationNames.moduleDescriptorFiles) {
        description = "Fetch Dokka Module files for $formatName from other subprojects"
        asConsumer()
        extendsFrom(dokkaConsumer.get())
        isVisible = false
        attributes {
          dokkaCategory(attributes.dokkaModuleFiles)
        }
      }

    val dokkaModuleOutgoing =
      configurations.register(configurationNames.moduleDescriptorFilesOutgoing) {
        description = "Provide Dokka Module files for $formatName to other subprojects"
        asProvider()
        // extend from dokkaConfigurationsConsumer, so Dokka Module Configs propagate api() style
        extendsFrom(dokkaModuleConsumer.get())
        isVisible = true
        attributes {
          dokkaCategory(attributes.dokkaModuleFiles)
        }
      }
    //</editor-fold>

    //<editor-fold desc="Dokka Generator Plugins">
    val dokkaPluginsClasspath =
      configurations.register(configurationNames.dokkaPluginsClasspath) {
        description = "Dokka Plugins classpath for $formatName"
        asConsumer()
        isVisible = false
        attributes {
          jvmJar()
          dokkaCategory(attributes.dokkaPluginsClasspath)
        }
      }

    val dokkaPluginsIntransitiveClasspath =
      configurations.register(configurationNames.dokkaPluginsIntransitiveClasspath) {
        description =
          "Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
        asConsumer()
        extendsFrom(dokkaPluginsClasspath.get())
        isVisible = false
        isTransitive = false
        attributes {
          jvmJar()
          dokkaCategory(attributes.dokkaPluginsClasspath)
        }
      }

    val dokkaPluginsClasspathOutgoing =
      configurations.register(configurationNames.dokkaPluginsClasspathOutgoing) {
        description = "Provide the Dokka Plugins classpath for $formatName to other subprojects"
        asProvider()
        extendsFrom(dokkaPluginsClasspath.get())
        isVisible = false
        attributes {
          jvmJar()
          dokkaCategory(attributes.dokkaPluginsClasspath)
        }
      }
    //</editor-fold>

    //<editor-fold desc="Dokka Generator Classpath">
    val dokkaGeneratorClasspath =
      configurations.register(configurationNames.dokkaGeneratorClasspath) {
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
          dokkaCategory(attributes.dokkaGeneratorClasspath)
        }
      }

//        configurations.register(configurationNames.dokkaGeneratorClasspath + "Elements") {
//            description = "Provide Dokka Generator classpath to other subprojects"
//            asProvider()
//            isVisible = true
//            attributes {
//                jvmJar()
//                dokkaCategory(attributes.dokkaGeneratorClasspath)
//            }
//            extendsFrom(dokkaGeneratorClasspath.get())
//        }
    //</editor-fold>

    return DokkatooFormatGradleConfigurations(
      dokkaModuleConsumer = dokkaModuleConsumer,
      dokkaModuleOutgoing = dokkaModuleOutgoing,
      dokkaPluginsClasspath = dokkaPluginsClasspath,
      dokkaGeneratorClasspath = dokkaGeneratorClasspath,
      dokkaPluginsIntransitiveClasspath = dokkaPluginsIntransitiveClasspath,
      dokkaPluginsClasspathOutgoing = dokkaPluginsClasspathOutgoing,
      dokkaParametersConsumer = dokkaParametersConsumer,
      dokkaParametersOutgoing = dokkaParametersOutgoing,
    )
  }


  private fun TaskContainer.createDokkaLifecycleTasks() {
    val prepareParameters = register<DokkatooTask>(TaskName.PREPARE_PARAMETERS) {
      description = "Runs all Dokkatoo Create Configuration tasks"
      dependsOn(withType<DokkatooPrepareParametersTask>())
    }

    register<DokkatooTask>(TaskName.GENERATE) {
      description = "Runs all Dokkatoo Generate tasks"
      dependsOn(prepareParameters)
      dependsOn(withType<DokkatooGenerateTask>())
    }
  }

  companion object {

    const val EXTENSION_NAME = "dokkatoo"

    /**
     * Names of the Gradle [Configuration]s used by the [Dokkatoo Plugin][DokkatooBasePlugin].
     *
     * Beware the confusing terminology:
     * - [Gradle Configurations][org.gradle.api.artifacts.Configuration] - share files between subprojects. Each has a name.
     * - [DokkaConfiguration][org.jetbrains.dokka.DokkaConfiguration] - parameters for executing the Dokka Generator
     */
    object ConfigurationName {

      const val DOKKATOO = "dokkatoo"

      /** Name of the [Configuration] that _consumes_ [dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs] from projects */
      const val DOKKATOO_PARAMETERS = "dokkatooParameters"

      /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration] to other projects */
      const val DOKKATOO_PARAMETERS_OUTGOING = "dokkatooParametersElements"

      /** Name of the [Configuration] that _consumes_ all [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] files */
      const val DOKKATOO_MODULE_FILES_CONSUMER = "dokkatooModule"

      /** Name of the [Configuration] that _provides_ all [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] files to other projects */
      const val DOKKATOO_MODULE_FILES_PROVIDER = "${DOKKATOO_MODULE_FILES_CONSUMER}Elements"

      /**
       * Classpath used to execute the Dokka Generator.
       *
       * Extends [DOKKA_PLUGINS_CLASSPATH], so Dokka plugins and their dependencies are included.
       */
      const val DOKKA_GENERATOR_CLASSPATH = "dokkatooGeneratorClasspath"

      /** Dokka Plugins (including transitive dependencies, so this can be passed to the Dokka Generator Worker classpath) */
      const val DOKKA_PLUGINS_CLASSPATH = "dokkatooPlugin"

      /**
       * Dokka Plugins (excluding transitive dependencies) will be used to create Dokka Generator Parameters
       *
       * Generally, this configuration should not be invoked manually. Instead, use [DOKKA_PLUGINS_CLASSPATH].
       */
      internal const val DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH =
        "${DOKKA_PLUGINS_CLASSPATH}Intransitive"

      /** _Provides_ Dokka Plugins Classpath to other subprojects */
      const val DOKKA_PLUGINS_CLASSPATH_OUTGOING = "${DOKKA_PLUGINS_CLASSPATH}Elements"
    }

    /**
     * The group of all Dokkatoo Gradle tasks.
     *
     * @see org.gradle.api.Task.getGroup
     */
    const val TASK_GROUP = "dokkatoo"

    object TaskName {
      const val GENERATE = "dokkatooGenerate"
      const val GENERATE_PUBLICATION = "${GENERATE}Publication"
      const val GENERATE_MODULE = "${GENERATE}Module"
      const val PREPARE_PARAMETERS = "prepareDokkatooParameters"
      const val PREPARE_MODULE_DESCRIPTOR = "prepareDokkatooModuleDescriptor"
    }

    internal val jsonMapper = Json {
      prettyPrint = true
      @OptIn(ExperimentalSerializationApi::class)
      prettyPrintIndent = "  "
    }
  }
}
