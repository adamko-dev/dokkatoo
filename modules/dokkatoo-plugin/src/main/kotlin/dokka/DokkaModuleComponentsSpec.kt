package dev.adamko.dokkatoo.dokka

import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleParametersKxs.SourceSetIdKxs
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetIdSpec
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import kotlinx.serialization.Serializable

@Serializable
@DokkatooInternalApi
data class DokkaModuleComponentsSpec(
  val moduleParametersFileName: String = FILE_NAME,
  val classpathDirName: String = "classpath",
  val samplesDirName: String = "samples",
  val includesDirName: String = "includes",
  val pluginsDirName: String = "plugins",
  val sourceSetsDirName: String = "sourceSets",
  val sourceSetDirNames: Map<String, String>,
) {
  fun sourceSetDirName(idSpec: DokkaSourceSetIdSpec): String {
    val id = SourceSetIdKxs(idSpec)
    return sourceSetDirNames[id.key] ?: error("missing sourceSetDirName for $idSpec")
  }

  @DokkatooInternalApi
  companion object {
    const val FILE_NAME = "dokka-module-components.json"
  }
}
