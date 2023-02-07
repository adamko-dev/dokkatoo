package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.DokkaPublication
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/**
 * Base Gradle Plugin for setting up a Dokka Publication for a specific format.
 *
 * [DokkatooBasePlugin] must be applied for this plugin (or any subclass) to have an effect.
 */
abstract class DokkatooFormatPlugin @Inject constructor(
  val formatName: String,
) : Plugin<Project> {

  final override fun apply(target: Project) {

    // apply DokkatooBasePlugin (does nothing if already applied)
    target.pluginManager.apply(DokkatooBasePlugin::class)

    target.plugins.withType<DokkatooBasePlugin>().configureEach {
      val dokkatooExtension = target.extensions.getByType(DokkatooExtension::class)

      val publication = dokkatooExtension.dokkatooPublications.create(formatName)

      val context = PublicationPluginContext(target, dokkatooExtension, publication)

      context.configure()
    }
  }

  class PublicationPluginContext(
    val project: Project,
    val dokkatooExtension: DokkatooExtension,
    val publication: DokkaPublication,
  ) {

    fun DependencyHandler.dokka(module: String) =
      dokkatooExtension.dokkaVersion.map { version -> create("org.jetbrains.dokka:$module:$version") }

    fun DependencyHandler.dokkaPlugin(dependency: Provider<Dependency>) =
      addProvider(publication.configurationNames.dokkaPluginsClasspath, dependency)

    fun DependencyHandler.dokkaPlugin(dependency: String) =
      add(publication.configurationNames.dokkaPluginsClasspath, dependency)

    fun DependencyHandler.dokkaGenerator(dependency: Provider<Dependency>) =
      addProvider(publication.configurationNames.dokkaGeneratorClasspath, dependency)

    fun DependencyHandler.dokkaGenerator(dependency: String) =
      add(publication.configurationNames.dokkaGeneratorClasspath, dependency)
  }

  /** Format specific configuration */
  open fun PublicationPluginContext.configure() {}
}
