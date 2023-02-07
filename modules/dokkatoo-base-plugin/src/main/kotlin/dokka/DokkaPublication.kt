package dev.adamko.dokkatoo.dokka

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_CONFIGURATIONS
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_CONFIGURATION_ELEMENTS
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_GENERATOR_CLASSPATH
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.TaskName.CREATE_CONFIGURATION
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.TaskName.CREATE_MODULE_CONFIGURATION
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.TaskName.GENERATE
import dev.adamko.dokkatoo.dokka.parameters.DokkaParametersGradleBuilder
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

/**
 * A [DokkaPublication] describes a single Dokka output.
 *
 * Each Publication has its own set of Gradle tasks and [org.gradle.api.artifacts.Configuration]s.
 *
 * The type of site is determined by the Dokka Plugins. By default, an HTML site will be generated.
 * By default, Dokka will create publications for HTML, Jekyll, and GitHub Flavoured Markdown.
 */
abstract class DokkaPublication @Inject constructor(
  @get:Internal
  val formatName: String,
) : Named, Serializable {

  @Internal
  override fun getName(): String = formatName

  @get:Internal
  abstract val description: Property<String>

  @get:Input
  abstract val enabled: Property<Boolean>

  @Internal
  val taskNames = TaskNames()

  @Internal
  val configurationNames = ConfigurationNames()

  inner class TaskNames : Serializable {
    val generate: String = GENERATE + formatName.capitalize()
    val createConfiguration: String = CREATE_CONFIGURATION + formatName.capitalize()
    val createModuleConfiguration: String =
      CREATE_MODULE_CONFIGURATION + formatName.capitalize()
  }

  inner class ConfigurationNames : Serializable {
    val dokkaConfigurations: String = DOKKATOO_CONFIGURATIONS + formatName.capitalize()
    val dokkaConfigurationElements: String = DOKKATOO_CONFIGURATION_ELEMENTS + formatName.capitalize()
    val dokkaGeneratorClasspath: String = DOKKA_GENERATOR_CLASSPATH + formatName.capitalize()
    val dokkaPluginsClasspath: String = DOKKA_PLUGINS_CLASSPATH + formatName.capitalize()
    val dokkaPluginsIntransitiveClasspath: String =
      DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH + formatName.capitalize()
  }


  @get:Nested
  abstract val dokkaConfiguration: DokkaParametersGradleBuilder
}
