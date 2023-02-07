package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.adapters.DokkatooKotlinAdapter
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.internal.versions
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Property
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

  override fun apply(target: Project) {

    // apply DokkatooBasePlugin
    target.pluginManager.apply(DokkatooBasePlugin::class)
    // apply the plugin that will autoconfigure Dokkatoo to use the sources of a Kotlin project
    target.pluginManager.apply(type = DokkatooKotlinAdapter::class)

    target.plugins.withType<DokkatooBasePlugin>().configureEach {
      val dokkatooExtension = target.extensions.getByType(DokkatooExtension::class)

      val publication = dokkatooExtension.dokkatooPublications.create(formatName)

      val context = PublicationPluginContext(target, dokkatooExtension, publication)

      context.configure()

      if (context.addDefaultDependencies) {
        with(context) {
          target.dependencies.addDefaultDependencies()
        }
      }
    }
  }

  class PublicationPluginContext(
    val project: Project,
    val dokkatooExtension: DokkatooExtension,
    val publication: DokkaPublication,
  ) {
    var addDefaultDependencies = true

    /** Create a [Dependency] for  */
    fun DependencyHandler.dokka(module: String): Provider<Dependency> =
      dokkatooExtension.versions.jetbrainsDokka.map { version -> create("org.jetbrains.dokka:$module:$version") }

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: Provider<Dependency>): Unit =
      addProvider(publication.configurationNames.dokkaPluginsClasspath, dependency)

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: String) {
      add(publication.configurationNames.dokkaPluginsClasspath, dependency)
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: Provider<Dependency>) {
      addProvider(publication.configurationNames.dokkaGeneratorClasspath, dependency)
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: String) {
      add(publication.configurationNames.dokkaGeneratorClasspath, dependency)
    }

    fun DependencyHandler.addDefaultDependencies() {

      // lazily create a Dependency
      infix fun String.version(version: Property<String>): Provider<Dependency> =
        version.map { v -> create("$this:$v") }

      with(dokkatooExtension.versions) {
        dokkaPlugin("org.jetbrains:markdown" version jetbrainsMarkdown)

        dokkaPlugin(dokka("kotlin-analysis-intellij"))
        dokkaPlugin(dokka("dokka-base"))
        dokkaPlugin(dokka("templating-plugin"))
        dokkaPlugin(dokka("dokka-analysis"))
        dokkaPlugin(dokka("kotlin-analysis-compiler"))

        dokkaPlugin("org.jetbrains.kotlinx:kotlinx-html" version kotlinxHtml)
        dokkaPlugin("org.freemarker:freemarker" version freemarker)

        dokkaGenerator(dokka("dokka-core"))
      }
    }
  }

  /** Format specific configuration - to be implemented by subclasses */
  open fun PublicationPluginContext.configure() {}
}
