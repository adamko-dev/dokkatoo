package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.adding
import dev.adamko.dokkatoo.internal.domainObjectContainer
import dev.adamko.dokkatoo.workers.WorkerIsolation
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE


/**
 * Gradle Worker specific parameters
 */
abstract class DokkaWorkerParameters : ExtensionAware {

  @get:Nested
  abstract val isolation: Property<WorkerIsolation>

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection
}


/**
 * Parameters that affect how source content is rendered.
 */
abstract class BaseDokkaRenderingParameters : ExtensionAware {
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val includes: ConfigurableFileCollection
  @get:Input
  abstract val suppressInheritedMembers: Property<Boolean>
  @get:Input
  abstract val suppressObviousFunctions: Property<Boolean>
  @get:Input
  abstract val moduleName: Property<String>
  @get:Input
  @get:Optional
  abstract val moduleVersion: Property<String>
  @get:Nested
  abstract val pluginParameters: DokkaPluginParametersContainer
  @get:Input
  abstract val enabledPluginIds: ListProperty<String>
}


abstract class DokkaPublicationRenderingParameters : BaseDokkaRenderingParameters(),
  ExtensionAware {

  @get:InputFiles
  abstract val moduleDirectories: ConfigurableFileCollection
}


abstract class DokkaModuleRenderingParameters @Inject constructor(
  objects: ObjectFactory
) : BaseDokkaRenderingParameters(), ExtensionAware {

  @get:Nested
  val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec> =
    extensions.adding("dokkaSourceSets", objects.domainObjectContainer())

}


/**
 * Parameters that control the behaviour of the Dokka Generator.
 *
 * Will only affect the behaviour of Dokka tasks in the current project.
 */
abstract class DokkaGeneratorParameters : ExtensionAware {
  @get:LocalState
  abstract val cacheDirectory: DirectoryProperty
  @get:Input
  abstract val finalizeCoroutines: Property<Boolean>
  @get:Input
  abstract val failOnWarning: Property<Boolean>
  @get:Input
  abstract val offlineMode: Property<Boolean>
  @get:LocalState
  abstract val logFile: RegularFileProperty
}
