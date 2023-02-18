package dev.adamko.dokkatoo.dokka.parameters

import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationBuilder

/**
 * @param[pluginFqn] Fully qualified classname of the Dokka Plugin
 */
abstract class DokkaPluginConfigurationSpec @Inject constructor(
  @get:Input
  val pluginFqn: String
) :
  DokkaConfigurationBuilder<DokkaParametersKxs.PluginConfigurationKxs>,
  Serializable,
  Named {

  @get:Input
  abstract val serializationFormat: Property<DokkaConfiguration.SerializationFormat>

  @get:Input
  abstract val values: Property<String>

  override fun build() = DokkaParametersKxs.PluginConfigurationKxs(
    fqPluginName = pluginFqn,
    serializationFormat = serializationFormat.get(),
    values = values.get(),
  )

  @Internal
  override fun getName(): String = pluginFqn
}
