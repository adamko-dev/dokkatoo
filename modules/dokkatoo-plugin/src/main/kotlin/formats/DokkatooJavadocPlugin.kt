package dev.adamko.dokkatoo.formats

import org.gradle.kotlin.dsl.dependencies

abstract class DokkatooJavadocPlugin : DokkatooFormatPlugin(formatName = "javadoc") {
  override fun DokkatooFormatPluginContext.configure() {
    project.dependencies {
      dokkaPlugin(dokka("javadoc-plugin"))
    }
  }
}
