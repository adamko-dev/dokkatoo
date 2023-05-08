package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import dev.adamko.dokkatoo.dokka.plugins.DokkaPluginParametersBaseSpec
import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.*

/**
 * Builds the Dokka Parameters that the Dokka Generator will use to produce a Dokka Publication for this project.
 *
 * Configurations from other modules (which are potentially from other Gradle subprojects) will also be included
 * via ... TODO explain how to include other subprojects/modules
 */
@CacheableTask
abstract class DokkatooPrepareParametersTask
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,

  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  @get:Nested
  val pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkatooTask.WithSourceSets() {

  @get:OutputFile
  abstract val dokkaConfigurationJson: RegularFileProperty

  /** Dokka Configuration files from other subprojects that will be merged into this Dokka Configuration */
  @get:InputFiles
//    @get:NormalizeLineEndings
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val dokkaSubprojectParameters: ConfigurableFileCollection

  /** Dokka Module files from other subprojects. */
  @get:InputFiles
//    @get:NormalizeLineEndings
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val dokkaModuleFiles: ConfigurableFileCollection

//  @get:LocalState
  // cacheRoot is not used by Dokka, and will probably be deprecated
//  abstract val cacheRoot: DirectoryProperty

  @get:Input
  abstract val delayTemplateSubstitution: Property<Boolean>

  @get:Input
  abstract val failOnWarning: Property<Boolean>

//  @get:InputFiles
//  @get:PathSensitive(RELATIVE)
//  abstract val includes: ConfigurableFileCollection

  @get:Input
  abstract val finalizeCoroutines: Property<Boolean>

  @get:Input
  abstract val moduleName: Property<String>

  @get:Input
  @get:Optional
  abstract val moduleVersion: Property<String>

  /**
   * Deprecated: this value was never used, because it was overridden in
   * [DokkatooGenerateTask].
   *
   * The output directory should instead be set using
   * [dev.adamko.dokkatoo.DokkatooExtension.dokkatooPublicationDirectory]
   * (or [dev.adamko.dokkatoo.DokkatooExtension.dokkatooModuleDirectory] for modules)
   */
  @Deprecated("This value was never used - see KDoc for alternative")
  @get:Internal
  val outputDir: DirectoryProperty = objects.directoryProperty()

  /** @see [outputDir] */
  @Deprecated("This value was never used - see outputDir")
  @get:Internal
  protected val outputDirPath: Provider<String> =
    @Suppress("DEPRECATION")
    outputDir.map { it.asFile.invariantSeparatorsPath }

  @get:Input
  abstract val offlineMode: Property<Boolean>

//  /**
//   * Classpath that contains the Dokka Generator Plugins used to modify this publication.
//   *
//   * The plugins should be configured in [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
//   */
//  @get:InputFiles
//  @get:Classpath
//  abstract val pluginsClasspath: ConfigurableFileCollection

  @get:Input
  abstract val suppressObviousFunctions: Property<Boolean>

  @get:Input
  abstract val suppressInheritedMembers: Property<Boolean>

  /** @see dev.adamko.dokkatoo.dokka.DokkaPublication.enabled */
  @get:Input
  abstract val publicationEnabled: Property<Boolean>

  init {
    description = "Assembles a Dokka configuration file, to be used when executing Dokka"
  }

  @TaskAction
  internal fun generateConfiguration() {
    val dokkaConfiguration = buildDokkaConfiguration()

    val encodedModuleDesc = jsonMapper.encodeToString(dokkaConfiguration)

    logger.info(encodedModuleDesc)

    dokkaConfigurationJson.get().asFile.writeText(encodedModuleDesc)
  }


  private fun buildDokkaConfiguration(): DokkaParametersKxs {

    val moduleName = moduleName.get()
    val moduleVersion = moduleVersion.orNull?.takeIf { it != "unspecified" }
//    val cacheRoot = cacheRoot.asFile.orNull
    val offlineMode = offlineMode.get()
    val sourceSets = dokkaSourceSets.filterNot {
      val suppressed = it.suppress.get()
      logger.info("Dokka source set ${it.sourceSetId.get()} ${if (suppressed) "is" else "isn't"} suppressed")
      suppressed
    }.map(DokkaSourceSetSpec::build)

//    val pluginsClasspath = pluginsClasspath.files.toList()

    val pluginsConfiguration = pluginsConfiguration.map(DokkaPluginParametersBaseSpec::build)
    val failOnWarning = failOnWarning.get()
    val delayTemplateSubstitution = delayTemplateSubstitution.get()
    val suppressObviousFunctions = suppressObviousFunctions.get()
//    val includes = includes.files
    val suppressInheritedMembers = suppressInheritedMembers.get()
    val finalizeCoroutines = finalizeCoroutines.get()

    val dokkaModuleDescriptors = dokkaModuleDescriptors()

    // construct the base configuration for THIS project
    val baseDokkaParams = DokkaParametersKxs(
      //cacheRoot = cacheRoot,
      delayTemplateSubstitution = delayTemplateSubstitution,
      failOnWarning = failOnWarning,
      finalizeCoroutines = finalizeCoroutines,
//      includes = includes,
      moduleName = moduleName,
      moduleVersion = moduleVersion,
      modules = dokkaModuleDescriptors,
      offlineMode = offlineMode,
//      pluginsClasspath = pluginsClasspath,
      pluginsConfiguration = pluginsConfiguration,
      sourceSets = sourceSets,
      suppressInheritedMembers = suppressInheritedMembers,
      suppressObviousFunctions = suppressObviousFunctions,
    )

    logger.debug("Generated baseDokkaParams: $baseDokkaParams")

    return baseDokkaParams

    // TODO 'Dokka Collect' replacement - merge subprojects directly, not as modules
    //// fetch parameters from OTHER subprojects
    //val subprojectConfigs = dokkaSubprojectParameters.files.map { file ->
    //  val fileContent = file.readText()
    //  jsonMapper.decodeFromString(DokkaParametersKxs.serializer(), fileContent)
    //}
    //
    //// now combine them:
    //return subprojectConfigs.fold(baseDokkaParams) { acc, _: DokkaParametersKxs ->
    //  acc.copy(
    //    //sourceSets = acc.sourceSets + it.sourceSets,
    //// TODO remove plugin classpath aggregation, plugin classpath should be shared via Gradle Configuration
    //     //      so Gradle can correctly de-duplicate jars
    //     pluginsClasspath = acc.pluginsClasspath + it.pluginsClasspath,
    //  )
    //
    //}
  }

  private fun dokkaModuleDescriptors(): List<DokkaParametersKxs.DokkaModuleDescriptionKxs> {
    return dokkaModuleFiles.asFileTree
      .matching { include("**/module_descriptor.json") }
      .files.map { file ->
        try {
          val fileContent = file.readText()
          jsonMapper.decodeFromString(
            DokkaParametersKxs.DokkaModuleDescriptionKxs.serializer(),
            fileContent,
          )
        } catch (ex: Exception) {
          throw IOException("Could not parse DokkaModuleDescriptionKxs from $file", ex)
        }
      }
  }
}
