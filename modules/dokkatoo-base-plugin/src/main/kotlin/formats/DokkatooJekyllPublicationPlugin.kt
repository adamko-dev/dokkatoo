package dev.adamko.dokkatoo.formats

import org.gradle.kotlin.dsl.dependencies

abstract class DokkatooJekyllPublicationPlugin : DokkatooPublicationBasePlugin(
  formatName = "jekyll"
) {
  override fun PublicationPluginContext.configure() {
    project.dependencies {
      dokkaPlugin(dokka("jekyll-plugin"))
    }
  }
}
