package dev.adamko.dokkatoo.dokka

import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.adding
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property

/**
 * A [DokkaPublication] describes a single Dokka output.
 *
 * Each Publication has its own set of Gradle tasks and [org.gradle.api.artifacts.Configuration]s.
 *
 * The type of site is determined by the Dokka Plugins. By default, an HTML site will be generated.
 * By default, Dokka will create publications for HTML, Jekyll, and GitHub Flavoured Markdown.
 */
abstract class DokkaPublication
@DokkatooInternalApi
@Inject
constructor(
  val formatName: String,

  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.DokkatooExtension.pluginsConfiguration].
   */
  pluginsConfiguration: DokkaPluginParametersContainer,
) : Named, Serializable, ExtensionAware {

  /** Configurations for Dokka Generator Plugins. */
  val pluginsConfiguration: DokkaPluginParametersContainer =
    extensions.adding("pluginsConfiguration", pluginsConfiguration)

  override fun getName(): String = formatName

  abstract val enabled: Property<Boolean>

  abstract val moduleName: Property<String>

  abstract val moduleVersion: Property<String>

  /** Renamed - use [outputDirectory] instead. */
  @Deprecated("Renamed to outputDirectory", ReplaceWith("outputDirectory"))
  abstract val outputDir: DirectoryProperty

  /** Output directory for the finished Dokka publication. */
  abstract val outputDirectory: DirectoryProperty

  /** Output directory for the partial Dokka module. */
  internal abstract val moduleOutputDirectory: DirectoryProperty

  abstract val cacheRoot: DirectoryProperty

  abstract val offlineMode: Property<Boolean>

  abstract val failOnWarning: Property<Boolean>

  @Deprecated("No longer used")
  abstract val delayTemplateSubstitution: Property<Boolean>

  abstract val suppressObviousFunctions: Property<Boolean>

  abstract val includes: ConfigurableFileCollection

  abstract val suppressInheritedMembers: Property<Boolean>

  // TODO probably not needed any more, since Dokka Generator now runs in an isolated JVM process
  abstract val finalizeCoroutines: Property<Boolean>
}
