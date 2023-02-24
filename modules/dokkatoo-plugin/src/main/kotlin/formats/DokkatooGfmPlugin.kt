package dev.adamko.dokkatoo.formats

import org.gradle.kotlin.dsl.dependencies

abstract class DokkatooGfmPlugin : DokkatooFormatPlugin(formatName = "gfm") {
  override fun DokkatooFormatPluginContext.configure() {
    project.dependencies {
      dokkaPlugin(dokka("gfm-plugin"))
    }
  }
}
