package dev.adamko.dokkatoo.formats

import org.gradle.kotlin.dsl.dependencies

abstract class DokkatooJavadocPublicationPlugin : DokkatooPublicationBasePlugin(
  formatName = "javadoc"
) {
  override fun PublicationPluginContext.configure() {
    project.dependencies {
      dokkaPlugin(dokka("javadoc-plugin"))
    }
  }
}
