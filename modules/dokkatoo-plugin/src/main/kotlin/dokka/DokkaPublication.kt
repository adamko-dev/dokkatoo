package dev.adamko.dokkatoo.dokka

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_MODULE_DESCRIPTORS_CONSUMER
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_MODULE_DESCRIPTOR_PROVIDER
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_MODULE_SOURCE_OUTPUT_CONSUMER
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_MODULE_SOURCE_OUTPUT_PROVIDER
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_PARAMETERS
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKATOO_PARAMETERS_OUTGOING
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_GENERATOR_CLASSPATH
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH_OUTGOING
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.ConfigurationName.DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.TaskName.GENERATE_MODULE
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.TaskName.GENERATE_PUBLICATION
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.TaskName.PREPARE_MODULE_DESCRIPTOR
import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.TaskName.PREPARE_PARAMETERS
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

  @get:Nested
  abstract val dokkaConfiguration: DokkaParametersGradleBuilder

  @Internal
  val taskNames = TaskNames()

  @Internal
  val configurationNames = ConfigurationNames()

  private fun String.formatSuffix() = this + formatName.capitalize()

  inner class TaskNames : Serializable {
    val generatePublication = GENERATE_PUBLICATION.formatSuffix()
    val generateModule = GENERATE_MODULE.formatSuffix()
    val prepareParameters = PREPARE_PARAMETERS.formatSuffix()
    val prepareModuleDescriptor = PREPARE_MODULE_DESCRIPTOR.formatSuffix()
  }

  // TODO rename 'outgoing' or 'provider' to 'shared'?

  inner class ConfigurationNames : Serializable {
    val dokkaParametersConsumer: String = DOKKATOO_PARAMETERS.formatSuffix()
    val dokkaParametersOutgoing: String = DOKKATOO_PARAMETERS_OUTGOING.formatSuffix()
    val moduleDescriptors = DOKKATOO_MODULE_DESCRIPTORS_CONSUMER.formatSuffix()
    val moduleDescriptorsOutgoing = DOKKATOO_MODULE_DESCRIPTOR_PROVIDER.formatSuffix()
    val moduleSourceOutputConsumer = DOKKATOO_MODULE_SOURCE_OUTPUT_CONSUMER.formatSuffix()
    val moduleSourceOutputOutgoing = DOKKATOO_MODULE_SOURCE_OUTPUT_PROVIDER.formatSuffix()
    val dokkaGeneratorClasspath = DOKKA_GENERATOR_CLASSPATH.formatSuffix()
    val dokkaPluginsClasspath = DOKKA_PLUGINS_CLASSPATH.formatSuffix()
    val dokkaPluginsIntransitiveClasspath = DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH.formatSuffix()
    val dokkaPluginsClasspathOutgoing = DOKKA_PLUGINS_CLASSPATH_OUTGOING.formatSuffix()
  }
}
