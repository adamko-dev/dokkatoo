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
    registerDokkaBasePluginConfiguration()
    registerDokkaVersioningPlugin()

    dokkatooFormatTasks.generatePublication.configure {
      doLast {
        val indexHtml =
          outputDirectory.asFile.orNull?.resolve("index.html")?.invariantSeparatorsPath
        logger.lifecycle("Generated Dokka HTML publication: file://$indexHtml")
      }
    }
  }

  private fun DokkatooFormatPluginContext.registerDokkaBasePluginConfiguration() {
    with(dokkatooExtension.pluginsConfiguration) {
      registerBinding(DokkaHtmlPluginParameters::class, DokkaHtmlPluginParameters::class)
      register<DokkaHtmlPluginParameters>(DOKKA_HTML_PARAMETERS_NAME)
      withType<DokkaHtmlPluginParameters>().configureEach {
        separateInheritedMembers.convention(false)
        mergeImplicitExpectActualDeclarations.convention(false)
      }
    }
  }

  private fun DokkatooFormatPluginContext.registerDokkaVersioningPlugin() {
    // register and configure Dokka Versioning Plugin
    with(dokkatooExtension.pluginsConfiguration) {
      registerBinding(
        DokkaVersioningPluginParameters::class,
        DokkaVersioningPluginParameters::class,
      )
      register<DokkaVersioningPluginParameters>(DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME)
      withType<DokkaVersioningPluginParameters>().configureEach {
        renderVersionsNavigationOnAllPages.convention(true)
      }
    }
  }
}
