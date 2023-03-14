package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.dokka.parameters.DokkaPluginConfigurationSpec.EncodedFormat.JSON
import dev.adamko.dokkatoo.dokka.parameters.DokkaPluginConfigurationSpec.EncodedFormat.XML
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.DokkaConfiguration

/**
 * @param[pluginFqn] Fully qualified classname of the Dokka Plugin
 */
abstract class DokkaPluginConfigurationSpec
@DokkatooInternalApi
@Inject
constructor(
  @get:Input
  val pluginFqn: String
) :
  DokkaParameterBuilder<DokkaParametersKxs.PluginConfigurationKxs>,
  Serializable,
  Named {

  @get:Input
  abstract val serializationFormat: Property<EncodedFormat>

  @get:Input
  abstract val values: Property<String>

  @DokkatooInternalApi
  override fun build() = DokkaParametersKxs.PluginConfigurationKxs(
    fqPluginName = pluginFqn,
    serializationFormat = serializationFormat.get().dokkaType,
    values = values.get(),
  )

  @Internal
  override fun getName(): String = pluginFqn

  /**
   * Denotes how a [DokkaPluginConfigurationSpec] will be encoded.
   *
   * This should typically be [JSON]. [XML] is intended for use with the Dokka Maven plugin.
   *
   * @see org.jetbrains.dokka.DokkaConfiguration.SerializationFormat
   */
  // TODO maybe remove XML? I'm not sure it's at all useful.
  enum class EncodedFormat(
    internal val dokkaType: DokkaConfiguration.SerializationFormat
  ) {
    JSON(DokkaConfiguration.SerializationFormat.JSON),
    XML(DokkaConfiguration.SerializationFormat.XML),
  }
}
