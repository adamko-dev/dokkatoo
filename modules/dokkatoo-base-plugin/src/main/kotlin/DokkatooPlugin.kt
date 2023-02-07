package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.formats.DokkatooGfmPlugin
import dev.adamko.dokkatoo.formats.DokkatooHtmlPlugin
import dev.adamko.dokkatoo.formats.DokkatooJavadocPlugin
import dev.adamko.dokkatoo.formats.DokkatooJekyllPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * Dokkatoo Gradle Plugin.
 *
 * Creates all necessary defaults to generate documentation for HTML, Jekyll, Markdown, and Javadoc formats.
 */
abstract class DokkatooPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    with(target.pluginManager) {
      apply(type = DokkatooBasePlugin::class)

      // auto-apply the custom format plugins
      apply(type = DokkatooGfmPlugin::class)
      apply(type = DokkatooHtmlPlugin::class)
      apply(type = DokkatooJavadocPlugin::class)
      apply(type = DokkatooJekyllPlugin::class)
    }
  }
}
