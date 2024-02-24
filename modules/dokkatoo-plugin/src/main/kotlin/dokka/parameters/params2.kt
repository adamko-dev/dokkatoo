package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.workers.WorkerIsolation
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

/**
 * Gradle Worker specific parameters
 */
interface DokkaWorkerParameters {
  val isolation: Property<WorkerIsolation>
  val dokkaPlugins: ConfigurableFileCollection
  val runtimeClasspath: ConfigurableFileCollection
}

/**
 * Parameters that affect how source content is rendered.
 */
interface BaseDokkaRenderingParameters {
  val includes: ConfigurableFileCollection
  val suppressInheritedMembers: Property<Boolean>
  val suppressObviousFunctions: Property<Boolean>
  @get:Input
  val moduleName: Property<String>

  @get:Input
  @get:Optional
  val moduleVersion: Property<String>

//  @get:Nested
//  val pluginsConfiguration: DokkaPluginParametersContainer
}

interface DokkaPublicationRenderingParameters : BaseDokkaRenderingParameters {
  val moduleDirectories: ConfigurableFileCollection
}

interface DokkaModuleRenderingParameters : BaseDokkaRenderingParameters {

}

/**
 * Parameters that control the behaviour of the Dokka Generator.
 *
 * Will only affect the behaviour of Dokka tasks in the current project.
 */
interface DokkaGeneratorParameters {
  val cacheDirectory: DirectoryProperty
  val finalizeCoroutines: Property<Boolean>
  val failOnWarning: Property<Boolean>
  val offlineMode: Property<Boolean>
  val logFile: RegularFileProperty
}
