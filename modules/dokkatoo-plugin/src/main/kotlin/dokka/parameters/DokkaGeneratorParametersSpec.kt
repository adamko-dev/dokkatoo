package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.*
import dev.adamko.dokkatoo.internal.adding
import dev.adamko.dokkatoo.internal.domainObjectContainer
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE

/**
 * Parameters used to run Dokka Generator to produce either a
 * Dokka Publication or a Dokka Module.
 */
abstract class DokkaGeneratorParametersSpec
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  @get:Nested
  val pluginsConfiguration: DokkaPluginParametersContainer,
) : ExtensionAware {

//  /** Dokka Configuration files from other subprojects that will be merged into this Dokka Configuration */
//  @get:InputFiles
//  //@get:NormalizeLineEndings
//  @get:PathSensitive(PathSensitivity.RELATIVE)
//  @get:Optional
//  abstract val dokkaSubprojectParameters: ConfigurableFileCollection

  @get:Input
  abstract val failOnWarning: Property<Boolean>

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

  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val includes: ConfigurableFileCollection

  /**
   * Classpath that contains the Dokka Generator Plugins used to modify this publication.
   *
   * The plugins should be configured in [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  @get:InputFiles
  @get:Classpath
  abstract val pluginsClasspath: ConfigurableFileCollection

  /**
   * Source sets used to generate a Dokka Module.
   *
   * The values are not used directly in this task, but they are required to be registered as a
   * task input for up-to-date checks
   */
  @get:Nested
  val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec> =
    extensions.adding("dokkaSourceSets", objects.domainObjectContainer())

  /** Dokka Module files from other subprojects. */
  @get:Internal
  @Deprecated("DokkatooPrepareModuleDescriptorTask was not compatible with relocatable Gradle Build Cache and has been replaced with a dark Gradle devilry. All references to DokkatooPrepareModuleDescriptorTask must be removed.")
  @Suppress("unused")
  abstract val dokkaModuleFiles: ConfigurableFileCollection

//  @get:Nested
//  abstract val moduleDescriptors: NamedDomainObjectContainer<DokkaModuleDescriptionSpec>

  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val moduleOutputDirectories: ConfigurableFileCollection
}

//
//
//private fun createModuleDescriptors(
//  formatDependencies: FormatDependenciesManager
//): NamedDomainObjectContainer<DokkaModuleDescriptionSpec> {
//  val incomingModuleDescriptors =
//    formatDependencies.moduleDirectory.incomingArtifacts.map { moduleOutputDirectoryArtifact ->
//      moduleOutputDirectoryArtifact.map { moduleDirArtifact ->
//        createModuleDescriptor(formatDependencies, moduleDirArtifact)
//      }
//    }
//
//  val dokkaModuleDescriptors = objects.domainObjectContainer<DokkaModuleDescriptionSpec>()
//  dokkaModuleDescriptors.addAllLater(incomingModuleDescriptors)
//  return dokkaModuleDescriptors
//}
//
//private fun createModuleDescriptor(
//  formatDependencies: FormatDependenciesManager,
//  moduleDirArtifact: ResolvedArtifactResult,
//): DokkaModuleDescriptionSpec {
//  fun missingAttributeError(name: String): Nothing =
//    error("missing $name in artifact:$moduleDirArtifact, variant:${moduleDirArtifact.variant}, attributes: ${moduleDirArtifact.variant.attributes.toMap()}")
//
//  val moduleName = moduleDirArtifact.variant.attributes[DokkatooAttribute.DokkatooModuleNameAttribute]
//    ?: missingAttributeError("DokkatooModuleNameAttribute")
//
//  val projectPath = moduleDirArtifact.variant.attributes[DokkatooAttribute.DokkatooModulePathAttribute]
//    ?: missingAttributeError("DokkatooModulePathAttribute")
//
//  val moduleDirectory = moduleDirArtifact.file
//
//  val includes: Provider<List<File>> =
//    formatDependencies.moduleIncludes.incomingArtifacts.map { artifacts ->
//      artifacts
//        .filter { artifact -> artifact.variant.attributes[DokkatooAttribute.DokkatooModuleNameAttribute] == moduleName }
//        .map(ResolvedArtifactResult::getFile)
//    }
//
//  return objects.newInstance<DokkaModuleDescriptionSpec>(moduleName.name).apply {
//    this.moduleDirectory.convention(layout.dir(providers.provider { moduleDirectory })) // https://github.com/gradle/gradle/issues/23708
//    this.includes.from(includes)
//    this.projectPath.convention(projectPath.name)
//  }
//}
