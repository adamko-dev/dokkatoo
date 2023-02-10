package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_BASE_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_CATEGORY_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKA_FORMAT_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooFormatGradleConfigurations
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.dokka.parameters.DokkaPluginConfigurationGradleBuilder
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetGradleBuilder
import dev.adamko.dokkatoo.formats.*
import dev.adamko.dokkatoo.internal.asConsumer
import dev.adamko.dokkatoo.internal.asProvider
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask.GenerationType.MODULE
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask.GenerationType.PUBLICATION
import dev.adamko.dokkatoo.tasks.DokkatooPrepareModuleDescriptorTask
import dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask
import dev.adamko.dokkatoo.tasks.DokkatooTask
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
        attribute(
          DOKKATOO_BASE_ATTRIBUTE,
          configurationAttributes.dokkatooBaseUsage
        )
      }
    }

    configureAllDokkaPublications(
      target,
      dokkatooExtension,
      dokkaConsumerConfiguration,
      configurationAttributes,
    )

    configureDokkaPublicationsDefaults(
      dokkatooExtension = dokkatooExtension,
    )


    target.tasks.withType<DokkatooGenerateTask>().configureEach {
      cacheDirectory.convention(dokkatooExtension.dokkatooCacheDirectory)
    }

    target.tasks.withType<DokkatooPrepareModuleDescriptorTask>().configureEach {
      moduleName.convention(dokkatooExtension.moduleNameDefault)
    }

    //target.tasks.withType<DokkaConfigurationTask>().configureEach {
    //}
  }


  private fun createExtension(project: Project): DokkatooExtension {
    return project.extensions.create<DokkatooExtension>(EXTENSION_NAME).apply {
      dokkatooCacheDirectory.convention(null)
      moduleNameDefault.convention(providers.provider { project.name })
      moduleVersionDefault.convention(providers.provider { project.version.toString() })
      sourceSetScopeDefault.convention(project.path)
      dokkatooPublicationDirectory.convention(layout.buildDirectory.dir("dokka"))
      dokkatooModuleDirectory.convention(layout.buildDirectory.dir("dokka-module"))
      dokkatooConfigurationsDirectory.convention(layout.buildDirectory.dir("dokka-config"))

      this.extensions.create<DokkatooExtension.Versions>("versions").apply {
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
      val prepareConfigurationTask = project.tasks.register<DokkatooPrepareParametersTask>(
        taskNames.prepareParameters
      ) {
        description =
          "Creates Dokka Configuration for executing the Dokka Generator for the $formatName publication"

        dokkaConfigurationJson.convention(
          dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/dokka_parameters.json")
        )

        // depend on Dokka Module Descriptors from other subprojects
        dokkaSubprojectParameters.from(
          gradleConfigurations.dokkaParametersConsumer.map { elements ->
            elements.incoming.artifactView { lenient(true) }.files
          }
        )

        // depend on Dokka Module Configurations from other subprojects
        dokkaModuleDescriptorFiles.from(
          gradleConfigurations.dokkaModuleDescriptorsConsumer.map { elements ->
            elements.incoming.artifactView { lenient(true) }.files
          }
        )

        publicationEnabled.convention(this@publication.enabled)
        onlyIf { publicationEnabled.getOrElse(true) }

        cacheRoot.set(dokkaConfiguration.cacheRoot)
        delayTemplateSubstitution.set(dokkaConfiguration.delayTemplateSubstitution)
        dokkaSourceSets.addAllLater(providers.provider { this@publication.dokkaConfiguration.dokkaSourceSets })
        failOnWarning.set(dokkaConfiguration.failOnWarning)
        finalizeCoroutines.set(dokkaConfiguration.finalizeCoroutines)
        includes.from(dokkaConfiguration.includes)
        moduleName.set(dokkaConfiguration.moduleName)
        moduleVersion.set(dokkaConfiguration.moduleVersion)
        offlineMode.set(dokkaConfiguration.offlineMode)
        outputDir.set(dokkaConfiguration.outputDir)
        pluginsClasspath.from(gradleConfigurations.dokkaPluginsIntransitiveClasspath)

        pluginsConfiguration.addAllLater(providers.provider { dokkaConfiguration.pluginsConfiguration })

        //<editor-fold desc="adapter for old DSL - to be removed">
        pluginsConfiguration.addAllLater(
          @Suppress("DEPRECATION")
          pluginsMapConfiguration.map { pluginConfig ->
            pluginConfig.map { (pluginId, pluginConfiguration) ->
              objects.newInstance<DokkaPluginConfigurationGradleBuilder>().apply {
                fqPluginName.set(pluginId)
                values.set(pluginConfiguration)
              }
            }
          }
        )
        pluginsConfiguration.configureEach {
          serializationFormat.convention(DokkaConfiguration.SerializationFormat.JSON)
        }
        //</editor-fold>

        suppressInheritedMembers.set(dokkaConfiguration.suppressInheritedMembers)
        suppressObviousFunctions.set(dokkaConfiguration.suppressObviousFunctions)

        dokkaSourceSets.configureEach {
          // TODO for some reason the conventions need to be set again
          analysisPlatform.convention(Platform.DEFAULT)
          displayName.convention("main")
          documentedVisibilities.convention(listOf(DokkaConfiguration.Visibility.PUBLIC))
          jdkVersion.convention(8)
          noAndroidSdkLink.convention(false)
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
        }
      }

      gradleConfigurations.dokkaParametersOutgoing.configure {
        outgoing {
          artifact(prepareConfigurationTask.flatMap { it.dokkaConfigurationJson })
        }
      }

      project.tasks.register<DokkatooGenerateTask>(taskNames.generatePublication) {
        description = "Executes the Dokka Generator, generating the $formatName publication"
        outputDirectory.convention(dokkatooExtension.dokkatooPublicationDirectory.dir(formatName))
        dokkaConfigurationJson.convention(prepareConfigurationTask.flatMap { it.dokkaConfigurationJson })
        runtimeClasspath.from(gradleConfigurations.dokkaGeneratorClasspath)
        generationType.set(PUBLICATION)
        dokkaModuleSourceDirectories.from(gradleConfigurations.dokkaModuleSourceOutputsConsumer)
      }

      val generateModule =
        project.tasks.register<DokkatooGenerateTask>(taskNames.generateModule) {
          description = "Executes the Dokka Generator, generating a $formatName module"
          outputDirectory.convention(dokkatooExtension.dokkatooModuleDirectory.dir(formatName))
          dokkaConfigurationJson.convention(prepareConfigurationTask.flatMap { it.dokkaConfigurationJson })
          runtimeClasspath.from(gradleConfigurations.dokkaGeneratorClasspath)
          generationType.set(MODULE)
        }

      val prepareModuleDescriptorTask =
        project.tasks.register<DokkatooPrepareModuleDescriptorTask>(taskNames.prepareModuleDescriptor) {
          description = "Prepares the Dokka Module Descriptor JSON"
          includes.from(dokkaConfiguration.includes)
          dokkaModuleDescriptorJson.convention(
            dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/module_descriptor.json")
          )
          sourceOutputDirectory(generateModule.flatMap { it.outputDirectory })
        }

      gradleConfigurations.dokkaModuleDescriptorsOutgoing.configure {
        outgoing {
          artifact(prepareModuleDescriptorTask.flatMap { it.dokkaModuleDescriptorJson })
        }
      }
      gradleConfigurations.dokkaModuleSourceOutputsOutgoing.configure {
        outgoing {
          artifact(generateModule.flatMap { it.outputDirectory })
        }
      }
    }
  }

  /** Set defaults in all [DokkaPublication]s */
  private fun configureDokkaPublicationsDefaults(
    dokkatooExtension: DokkatooExtension,
  ) {

    fun DokkaSourceSetGradleBuilder.configureDefaults() {
      analysisPlatform.convention(Platform.DEFAULT)
      displayName.convention("main")
      documentedVisibilities.convention(listOf(DokkaConfiguration.Visibility.PUBLIC))
      jdkVersion.convention(8)
      noAndroidSdkLink.convention(false)
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
    }

    dokkatooExtension.dokkatooSourceSets.configureEach {
      configureDefaults()
    }
    dokkatooExtension.dokkatooPublications.configureEach {

      enabled.convention(true)

      dokkaConfiguration.apply {

        cacheRoot.convention(dokkatooExtension.dokkatooCacheDirectory)
        delayTemplateSubstitution.convention(false)
        failOnWarning.convention(false)
        finalizeCoroutines.convention(false)
        moduleName.convention(dokkatooExtension.moduleNameDefault)
        moduleVersion.convention(dokkatooExtension.moduleVersionDefault)
        offlineMode.convention(false)
        outputDir.convention(dokkatooExtension.dokkatooPublicationDirectory)
        suppressInheritedMembers.convention(false)
        suppressObviousFunctions.convention(true)

        // 'inherit' the common source sets defined in the extension
        dokkaSourceSets.addAllLater(
          objects.listProperty<DokkaSourceSetGradleBuilder>().apply {
            addAll(
              providers.provider { dokkatooExtension.dokkatooSourceSets }
            )
          }
        )

        // and configure each source set with the defaults
        dokkaSourceSets.configureEach {
          configureDefaults()
        }

        pluginsConfiguration.configureEach {
          serializationFormat.convention(DokkaConfiguration.SerializationFormat.JSON)
        }
      }
    }
  }

  // I don't think Dokka Modules are necessary - but they might make a return
//    private fun TaskContainer.registerDokkaModuleConfigurationTask(
//        dokkaGenerateTask: TaskProvider<DokkaGenerateTask>,
//    ): TaskProvider<DokkaModuleConfigurationTask> {
//        val dokkaModuleConfigurationTask =
//            register<DokkaModuleConfigurationTask>(TaskName.CREATE_DOKKA_MODULE_CONFIGURATION)
//
//        withType<DokkaModuleConfigurationTask>().configureEach {
//            moduleName.set(project.name.map { it.takeIf(Char::isLetterOrDigit) ?: "-" }.joinToString(""))
//            dokkaModuleConfigurationJson.set(
//                moduleName.flatMap { moduleName ->
//                    layout.buildDirectory.file("dokka/$moduleName.json")
//                }
//            )
//
//            dependsOn(dokkaGenerateTask)
//
////            moduleOutputDirectoryPath(dokkaGenerateTask.map { it.outputDirectory })
//            sourceOutputDirectory(dokkaGenerateTask.map { it.outputDirectory })
////            sourceOutputDirectory(layout.buildDirectory.dir("dokka/source-output"))
//        }
//
//        return dokkaModuleConfigurationTask
//    }

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

    //<editor-fold desc="Dokka Module Descriptor JSON files">
    val dokkaModuleDescriptorsConsumer =
      configurations.register(configurationNames.moduleDescriptors) {
        description = "Fetch Dokka Module Descriptors for $formatName from other subprojects"
        asConsumer()
        extendsFrom(dokkaConsumer.get())
        isVisible = false
        attributes {
          dokkaCategory(attributes.dokkaModuleDescriptors)
        }
      }

    val dokkaModuleDescriptorsOutgoing =
      configurations.register(configurationNames.moduleDescriptorsOutgoing) {
        description = "Provide Dokka Module Descriptors for $formatName to other subprojects"
        asProvider()
        // extend from dokkaConfigurationsConsumer, so Dokka Module Configs propagate api() style
        extendsFrom(dokkaModuleDescriptorsConsumer.get())
        isVisible = true
        attributes {
          dokkaCategory(attributes.dokkaModuleDescriptors)
        }
      }
    //</editor-fold>

    //<editor-fold desc="Dokka Module Descriptor JSON files">
    val dokkaModuleSourceOutputsConsumer =
      configurations.register(configurationNames.moduleSourceOutputConsumer) {
        description = "Fetch Dokka Module Source Output for $formatName from other subprojects"
        asConsumer()
        extendsFrom(dokkaConsumer.get())
        isVisible = false
        attributes {
          dokkaCategory(attributes.dokkaModuleSource)
        }
      }

    val dokkaModuleSourceOutputsOutgoing =
      configurations.register(configurationNames.moduleSourceOutputOutgoing) {
        description = "Provide Dokka Module Source Output for $formatName to other subprojects"
        asProvider()
        // extend from dokkaConfigurationsConsumer, so Dokka Module Configs propagate api() style
        extendsFrom(dokkaModuleDescriptorsConsumer.get())
        isVisible = true
        attributes {
          dokkaCategory(attributes.dokkaModuleSource)
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
//            dokkaConsumer = dokkaConsumer,
//      dokkaParametersConsumer = dokkaConfigurationsConsumer,
//      dokkaParametersProvider = dokkaConfigurationsProvider,
      dokkaModuleDescriptorsConsumer = dokkaModuleDescriptorsConsumer,
      dokkaModuleDescriptorsOutgoing = dokkaModuleDescriptorsOutgoing,
      dokkaPluginsClasspath = dokkaPluginsClasspath,
      dokkaGeneratorClasspath = dokkaGeneratorClasspath,
      dokkaPluginsIntransitiveClasspath = dokkaPluginsIntransitiveClasspath,
      dokkaPluginsClasspathOutgoing = dokkaPluginsClasspathOutgoing,
      dokkaModuleSourceOutputsConsumer = dokkaModuleSourceOutputsConsumer,
      dokkaModuleSourceOutputsOutgoing = dokkaModuleSourceOutputsOutgoing,
      dokkaParametersConsumer = dokkaParametersConsumer,
      dokkaParametersOutgoing = dokkaParametersOutgoing,
    )
  }


  private fun TaskContainer.createDokkaLifecycleTasks() {
    register(TaskName.GENERATE, DokkatooTask::class) {
      description = "Runs all Dokkatoo Generate tasks"
      dependsOn(withType<DokkatooGenerateTask>())
    }

    register(TaskName.PREPARE_PARAMETERS, DokkatooTask::class) {
      description = "Runs all Dokkatoo Create Configuration tasks"
      dependsOn(withType<DokkatooPrepareParametersTask>())
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

      /** Name of the [Configuration] that _consumes_ [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] from projects */
      const val DOKKATOO_MODULE_DESCRIPTORS_CONSUMER = "dokkatooModuleDescriptors"

      /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] to other projects */
      const val DOKKATOO_MODULE_DESCRIPTOR_PROVIDER =
        "${DOKKATOO_MODULE_DESCRIPTORS_CONSUMER}Elements"


      const val DOKKATOO_MODULE_SOURCE_OUTPUT_CONSUMER = "dokkatooModuleSource"
      const val DOKKATOO_MODULE_SOURCE_OUTPUT_PROVIDER = "dokkatooModuleSourceElements"

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
