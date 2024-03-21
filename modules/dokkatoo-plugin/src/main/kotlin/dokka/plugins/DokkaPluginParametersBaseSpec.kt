package dev.adamko.dokkatoo.dokka.plugins

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.File
import java.io.Serializable
import javax.inject.Inject
import kotlinx.serialization.KSerializer
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.tasks.Input

/**
 * Base class for defining Dokka Plugin configuration.
 *
 * This class should not be instantiated directly. Instead, use a subclass, or create plugin
 * parameters dynamically using [DokkaPluginParametersBuilder].
 *
 * [More information about Dokka Plugins is available in the Dokka docs.](https://kotlinlang.org/docs/dokka-plugins.html)
 *
 * @param[pluginFqn] Fully qualified classname of the Dokka Plugin
 */
abstract class DokkaPluginParametersBaseSpec
@DokkatooInternalApi
@Inject
constructor
  (
  private val name: String,
  @get:Input
  open val pluginFqn: String,
) : Serializable, Named {

//  abstract fun <T : DokkaPluginParametersBaseSpec> valuesSerializer(
//    componentsDir: File
//  ): KSerializer<T>

  abstract fun jsonEncode(): String // must be implemented by subclasses

  abstract fun createVariants(project: Project)
  abstract fun resolveVariants(project: Project)

  @Input
  override fun getName(): String = name
}
