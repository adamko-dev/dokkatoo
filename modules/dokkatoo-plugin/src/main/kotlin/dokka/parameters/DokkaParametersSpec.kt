package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.dokka.plugins.DokkaPluginParametersBaseSpec
import dev.adamko.dokkatoo.internal.*
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.*

/**
 * Spec class for building [org.jetbrains.dokka.DokkaConfiguration]
 *
 * (To try and prevent confusion with [org.gradle.api.artifacts.Configuration] we use the term
 * 'parameters' to describe settings that are passed to Dokka Generator.)
 */
abstract class DokkaParametersSpec
@DokkatooInternalApi
@Inject
constructor(
  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  @get:Nested
  val pluginsConfiguration: DokkaPluginParametersContainer,
  private val objects: ObjectFactory,
) :
  DokkaParameterBuilder<DokkaParametersKxs>,
  Serializable,
  ExtensionAware {

  /** Dokka Module descriptors */
  @get:Internal
  abstract val moduleDescriptors: ConfigurableFileCollection

  /**
   * Decoded from [moduleDescriptors], (used for normalizing input values)
   */
  @DokkatooInternalApi
  @get:Nested
  abstract val moduleDescriptorSpecs: ListProperty<DokkaModuleDescriptionSpec>

  @get:Input
  abstract val failOnWarning: Property<Boolean>

  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val includes: ConfigurableFileCollection

  @get:Input
  abstract val finalizeCoroutines: Property<Boolean>

  @get:Input
  abstract val moduleName: Property<String>

  @get:Input
  @get:Optional
  abstract val moduleVersion: Property<String>

  @get:Input
  abstract val offlineMode: Property<Boolean>

  @get:Input
  abstract val suppressObviousFunctions: Property<Boolean>

  @get:Input
  abstract val suppressInheritedMembers: Property<Boolean>

  @get:Nested
  abstract val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec>

  @DokkatooInternalApi
  override fun build(): DokkaParametersKxs {
    val moduleName = moduleName.get()
    val moduleVersion = moduleVersion.orNull?.takeIf { it != "unspecified" }
    val offlineMode = offlineMode.get()
    val sourceSets = dokkaSourceSets.filterNot {
      val suppressed = it.suppress.get()
      logger.info("Dokka source set ${it.sourceSetId.get()} ${if (suppressed) "is" else "isn't"} suppressed")
      suppressed
    }.map(DokkaSourceSetSpec::build)
    val pluginsConfiguration = pluginsConfiguration.map(DokkaPluginParametersBaseSpec::build)
    val failOnWarning = failOnWarning.get()
    val suppressObviousFunctions = suppressObviousFunctions.get()
//    val includes = includes.files
    val suppressInheritedMembers = suppressInheritedMembers.get()
    val finalizeCoroutines = finalizeCoroutines.get()
    val dokkaModuleDescriptors =
      moduleDescriptorSpecs.get().map(DokkaModuleDescriptionSpec::build)

    return DokkaParametersKxs(
      failOnWarning = failOnWarning,
      finalizeCoroutines = finalizeCoroutines,
      moduleName = moduleName,
      moduleVersion = moduleVersion,
      modules = dokkaModuleDescriptors,
      offlineMode = offlineMode,
      pluginsConfiguration = pluginsConfiguration,
      sourceSets = sourceSets,
      suppressInheritedMembers = suppressInheritedMembers,
      suppressObviousFunctions = suppressObviousFunctions,
    )
  }

  private companion object {
    private val logger = Logging.getLogger(DokkaParametersSpec::class.java)
  }
}
