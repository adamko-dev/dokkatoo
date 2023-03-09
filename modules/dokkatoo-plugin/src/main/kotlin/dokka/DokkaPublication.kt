package dev.adamko.dokkatoo.dokka

import dev.adamko.dokkatoo.dokka.parameters.DokkaPluginConfigurationSpec
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE

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

  @get:Nested
  abstract val pluginsConfiguration: NamedDomainObjectContainer<DokkaPluginConfigurationSpec>

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
  @get:PathSensitive(RELATIVE)
  abstract val includes: ConfigurableFileCollection

  @get:Input
  abstract val suppressInheritedMembers: Property<Boolean>

  @get:Input
  // TODO probably not needed any more, since Dokka Generator now runs in an isolated JVM process
  abstract val finalizeCoroutines: Property<Boolean>
}
