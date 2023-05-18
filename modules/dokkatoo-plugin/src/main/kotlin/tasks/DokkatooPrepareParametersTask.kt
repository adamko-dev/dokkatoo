package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.polymorphicDomainObjectContainer
import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.*
import org.gradle.work.DisableCachingByDefault

/**
 * Builds the Dokka Parameters that the Dokka Generator will use to produce a Dokka Publication for this project.
 *
 * Configurations from other modules (which are potentially from other Gradle subprojects) will also be included
 * via ... TODO explain how to include other subprojects/modules
 */
@Deprecated("merged into DokkatooGenerateTask")
@DisableCachingByDefault(because = "This task has been deprecated")
abstract class DokkatooPrepareParametersTask
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
) : DokkatooTask.WithSourceSets(objects) {

  @get:OutputFile
  abstract val dokkaConfigurationJson: RegularFileProperty

  /** Dokka Configuration files from other subprojects that will be merged into this Dokka Configuration */
  @get:InputFiles
  //@get:NormalizeLineEndings
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val dokkaSubprojectParameters: ConfigurableFileCollection

  /** Dokka Module files from other subprojects. */
  @get:InputFiles
  //@get:NormalizeLineEndings
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val dokkaModuleFiles: ConfigurableFileCollection

  @get:Input
  abstract val failOnWarning: Property<Boolean>

  @get:Input
  abstract val finalizeCoroutines: Property<Boolean>

  @get:Input
  abstract val moduleName: Property<String>

  @get:Input
  @get:Optional
  abstract val moduleVersion: Property<String>

  @get:Input
  abstract val offlineMode: Property<Boolean>

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
    logger.warn("Task $path has been deprecated")
  }

  //region Deprecated Properties
  @Deprecated("No longer used")
  @get:Internal
  abstract val cacheRoot: DirectoryProperty

  @Deprecated("No longer used")
  @get:Internal
  abstract val delayTemplateSubstitution: Property<Boolean>

  @Deprecated("No longer used")
  @get:Internal
  abstract val pluginsClasspath: ConfigurableFileCollection

  @Deprecated("No longer used")
  @get:Internal
  abstract val includes: ConfigurableFileCollection

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

  @Deprecated("No longer used")
  @get:Internal
  val pluginsConfiguration: DokkaPluginParametersContainer =
    objects.polymorphicDomainObjectContainer()
  //endregion
}
