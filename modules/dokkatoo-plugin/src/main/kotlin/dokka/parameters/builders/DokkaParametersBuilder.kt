package dev.adamko.dokkatoo.dokka.parameters.builders

import dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionSpec
import dev.adamko.dokkatoo.dokka.plugins.DokkaPluginParametersBaseSpec
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.mapToSet
import java.io.File
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.dokka.*

/**
 * Convert the Gradle-focused [DokkaGeneratorParametersSpec] into a [DokkaSourceSetImpl] instance,
 * which will be passed to Dokka Generator.
 *
 * The conversion is defined in a separate class to try and prevent classes from Dokka Generator
 * leaking into the public API.
 */
@DokkatooInternalApi
internal object DokkaParametersBuilder {

  fun build(
    spec: DokkaGeneratorParametersSpec,
    delayTemplateSubstitution: Boolean,
    outputDirectory: File,
    cacheDirectory: File? = null,
    moduleDescriptors: NamedDomainObjectContainer<DokkaModuleDescriptionSpec>,
  ): DokkaConfiguration {
    val moduleName = spec.moduleName.get()
    val moduleVersion = spec.moduleVersion.orNull?.takeIf { it != Project.DEFAULT_VERSION }
    val offlineMode = spec.offlineMode.get()
    val sourceSets = DokkaSourceSetBuilder.buildAll(spec.dokkaSourceSets)
    val failOnWarning = spec.failOnWarning.get()
    val suppressObviousFunctions = spec.suppressObviousFunctions.get()
    val suppressInheritedMembers = spec.suppressInheritedMembers.get()
    val finalizeCoroutines = spec.finalizeCoroutines.get()
    val pluginsConfiguration = spec.pluginsConfiguration.toSet()

    val pluginsClasspath = spec.pluginsClasspath.files.toList()
    val includes = spec.includes.files

    return DokkaConfigurationImpl(
      moduleName = moduleName,
      moduleVersion = moduleVersion,
      outputDir = outputDirectory,
      cacheRoot = cacheDirectory,
      offlineMode = offlineMode,
      sourceSets = sourceSets,
      pluginsClasspath = pluginsClasspath,
      pluginsConfiguration = pluginsConfiguration.map(::build),
      modules = build(moduleDescriptors),
      failOnWarning = failOnWarning,
      delayTemplateSubstitution = delayTemplateSubstitution,
      suppressObviousFunctions = suppressObviousFunctions,
      includes = includes,
      suppressInheritedMembers = suppressInheritedMembers,
      finalizeCoroutines = finalizeCoroutines,
    )
  }

  private fun build(
    moduleDescriptors: NamedDomainObjectContainer<DokkaModuleDescriptionSpec>
  ): List<DokkaModuleDescriptionImpl> =
    moduleDescriptors
      .map(::build)
      // Sort so the output is stable.
      // `relativePathToOutputDirectory` is better than `name` since it's guaranteed to be unique
      // across all modules (otherwise they'd be generated into the same directory), and even
      // though it's a file - it's a _relative_ file, so the ordering should be stable across
      // machines (which is important for relocatable Build Cache).
      .sortedBy { it.relativePathToOutputDirectory }

  private fun build(spec: DokkaModuleDescriptionSpec): DokkaModuleDescriptionImpl {
    val moduleDirectory = spec.moduleDirectory
      .asFile.orNull
      ?: error("missing required moduleDirectory in DokkaModuleDescriptionSpec(${spec.name})")

    val moduleIncludes = spec.includes
      .elements.orNull
      ?.mapToSet { it.asFile }
      ?: emptySet() // 'include' files are optional

    val projectPath = spec.projectPath.orNull?.ifBlank { null }
      ?: error("missing required projectPath in DokkaModuleDescriptionSpec(${spec.name})")

    // `relativeOutputDir` is the path where the Dokka Module should be located within the final
    // Dokka Publication.
    // Convert a project path e.g. `:x:y:z:my-cool-subproject` to a relative path e.g. `x/y/z/my-cool-subproject`.
    // The path has to be unique per module - using the project path is a useful way to achieve this.
    val relativeOutputDir = File(projectPath.removePrefix(":").replace(':', '/'))

    return DokkaModuleDescriptionImpl(
      name = spec.moduleName,
      relativePathToOutputDirectory = relativeOutputDir,
      includes = moduleIncludes,
      sourceOutputDirectory = moduleDirectory,
    )
  }

  private fun build(spec: DokkaPluginParametersBaseSpec): PluginConfigurationImpl {
    return PluginConfigurationImpl(
      fqPluginName = spec.pluginFqn,
      serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
      values = spec.jsonEncode(),
    )
  }
}
