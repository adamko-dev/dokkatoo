package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.internal.DOKKATOO_GRADLE_PLUGIN_DEPRECATION_MESSAGE
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.kotlin.dsl.*

@Deprecated(DOKKATOO_GRADLE_PLUGIN_DEPRECATION_MESSAGE)
@Suppress("DEPRECATION")
abstract class DokkatooJekyllPlugin
@DokkatooInternalApi
constructor() : DokkatooFormatPlugin(formatName = "jekyll") {
  override fun DokkatooFormatPluginContext.configure() {
    project.dependencies {
      dokkaPlugin(dokka("jekyll-plugin"))
    }
  }
}
