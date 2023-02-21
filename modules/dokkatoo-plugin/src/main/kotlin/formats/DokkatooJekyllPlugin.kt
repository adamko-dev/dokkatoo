package dev.adamko.dokkatoo.formats

import org.gradle.kotlin.dsl.dependencies

abstract class DokkatooJekyllPlugin : DokkatooFormatPlugin(formatName = "jekyll") {
  override fun DokkatooFormatPluginContext.configure() {
    project.dependencies {
      dokkaPlugin(dokka("jekyll-plugin"))
    }
  }
}
