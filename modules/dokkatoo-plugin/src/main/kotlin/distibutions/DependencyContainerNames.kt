package dev.adamko.dokkatoo.distributions

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import internal.HasFormatName
import org.gradle.api.artifacts.Configuration


/**
 * Names of the Gradle [Configuration]s used by the [Dokkatoo Plugin][DokkatooBasePlugin].
 *
 * Beware the confusing terminology:
 * - [Gradle Configurations][org.gradle.api.artifacts.Configuration] - share files between subprojects. Each has a name.
 * - [DokkaConfiguration][org.jetbrains.dokka.DokkaConfiguration] - parameters for executing the Dokka Generator
 */
@DokkatooInternalApi
class DependencyContainerNames(
  override val formatName: String?
) : HasFormatName {

  //val dokkatoo = "dokkatoo".appendFormat()

  val dokkatooModule = "dokkatooModule".appendFormat()
  val dokkatooModuleElements = "dokkatooModuleElements".appendFormat()

  val dokkatooSourceSets = "dokkatooSourceSets".appendFormat()
  val dokkatooSourceSetsElements = "dokkatooSourceSetsElements".appendFormat()

//  /** Name of the [Configuration] that _consumes_ [dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs] from projects */
//  val dokkatooParametersConsumer = "dokkatooParameters".appendFormat()

//  /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration] to other projects */
//  val dokkatooParametersOutgoing = "dokkatooParametersElements".appendFormat()

//  /** Name of the [Configuration] that _consumes_ all [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] files */
//  val dokkatooModuleFilesConsumer = "dokkatooModule".appendFormat()

//  /** Name of the [Configuration] that _provides_ all [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] files to other projects */
//  val dokkatooModuleFilesProvider = "dokkatooModuleElements".appendFormat()

  /**
   * Classpath used to execute the Dokka Generator.
   *
   * Extends [dokkaPluginsClasspath], so Dokka plugins and their dependencies are included.
   */
  val dokkaGeneratorClasspath = "dokkatooGeneratorClasspath".appendFormat()

  /** Dokka Plugins (including transitive dependencies, so this can be passed to the Dokka Generator Worker classpath) */
  val dokkaPluginsClasspath = "dokkatooPlugins".appendFormat()

  /**
   * Dokka Plugins (excluding transitive dependencies) will be used to create Dokka Generator Parameters
   *
   * Generally, this configuration should not be invoked manually. Instead, use [dokkaPluginsClasspath].
   */
  val dokkaPluginsIntransitiveClasspath = "dokkatooPluginIntransitive".appendFormat()
}
