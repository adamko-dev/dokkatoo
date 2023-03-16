package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters
import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters.Companion.DOKKA_HTML_PARAMETERS_NAME
import dev.adamko.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters
import dev.adamko.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters.Companion.DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.kotlin.dsl.*

abstract class DokkatooHtmlPlugin
@DokkatooInternalApi
constructor() : DokkatooFormatPlugin(formatName = "html") {

  override fun DokkatooFormatPluginContext.configure() {
    configureDokkaHtmlPlugins()
  }

  private fun DokkatooFormatPluginContext.configureDokkaHtmlPlugins() {
    with(dokkatooExtension.pluginsConfiguration) {
      registerFactory(DokkaHtmlPluginParameters::class.java) { named ->
        objects.newInstance(named)
      }
      register<DokkaHtmlPluginParameters>(DOKKA_HTML_PARAMETERS_NAME)
      withType<DokkaHtmlPluginParameters>().configureEach {
        separateInheritedMembers.convention(false)
        mergeImplicitExpectActualDeclarations.convention(false)
      }

      registerFactory(DokkaVersioningPluginParameters::class.java) { named ->
        objects.newInstance(named)
      }
      register<DokkaVersioningPluginParameters>(DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME)
      withType<DokkaVersioningPluginParameters>().configureEach {
        renderVersionsNavigationOnAllPages.convention(true)
      }
    }
  }
}
