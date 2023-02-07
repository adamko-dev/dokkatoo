package dev.adamko.dokkatoo.formats

import org.gradle.kotlin.dsl.dependencies

abstract class DokkatooGfmPublicationPlugin : DokkatooPublicationBasePlugin(
  formatName = "gfm"
) {
  override fun PublicationPluginContext.configure() {
    project.dependencies {
      dokkaPlugin(dokka("gfm-plugin"))
    }
  }
}
