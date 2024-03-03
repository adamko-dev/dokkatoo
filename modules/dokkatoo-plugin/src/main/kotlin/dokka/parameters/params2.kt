package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.workers.WorkerIsolation
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE



/**
 * Parameters that affect how source content is rendered.
 *
 * (Will be transferred between subprojects.)
 */
abstract class DokkaModuleRenderingParameters : ExtensionAware {

  @get:Input
  abstract val moduleName: Property<String>

  @get:Input
  @get:Optional
  abstract val moduleVersion: Property<String>

  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val includes: ConfigurableFileCollection

  @get:Input
  abstract val suppressInheritedMembers: Property<Boolean>

  @get:Input
  abstract val suppressObviousFunctions: Property<Boolean>
}


//abstract class DokkaPublicationRenderingParameters : BaseDokkaRenderingParameters(),
//  ExtensionAware {
//
//  @get:InputFiles
//  abstract val moduleDirectories: ConfigurableFileCollection
//}
//
//
//abstract class DokkaModuleRenderingParameters @Inject constructor(
//  objects: ObjectFactory
//) : BaseDokkaRenderingParameters(), ExtensionAware {
//
//  @get:Nested
//  val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec> =
//    extensions.adding("dokkaSourceSets", objects.domainObjectContainer())
//}


/**
 * Parameters that control the behaviour of the Dokka Generator.
 *
 * Will only affect the behaviour of Dokka tasks in the current project.
 *
 * (Not transferred between subprojects.)
 */
abstract class DokkaGeneratorParameters : ExtensionAware {

  @get:Nested
  abstract val pluginParameters: DokkaPluginParametersContainer

  @get:Input
  abstract val enabledPluginIds: ListProperty<String>

  @get:Input
  abstract val finalizeCoroutines: Property<Boolean>

  @get:Input
  abstract val failOnWarning: Property<Boolean>

  @get:Input
  abstract val offlineMode: Property<Boolean>

  @get:LocalState
  abstract val cacheDirectory: DirectoryProperty

  @get:LocalState
  abstract val logFile: RegularFileProperty

  @get:Nested
  abstract val isolation: Property<WorkerIsolation>

  /**
   * Classpath required to run Dokka Generator.
   *
   * Contains the Dokka Generator, Dokka plugins, and any transitive dependencies.
   */
  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection
}
