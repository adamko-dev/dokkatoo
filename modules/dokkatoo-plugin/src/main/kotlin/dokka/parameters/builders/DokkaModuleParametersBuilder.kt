package dev.adamko.dokkatoo.dokka.parameters.builders

import dev.adamko.dokkatoo.dokka.DokkaModuleComponentsSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleParametersKxs
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleParametersKxs.*
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleParametersKxs.SourceSetIdKxs.Companion.resolve
import dev.adamko.dokkatoo.dokka.parameters.KotlinPlatform.Companion.dokkaType
import dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier.Companion.dokkaType
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.mapToSet
import java.io.File
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.dokka.*

internal class DokkaModuleParametersBuilder {

  fun build(
    moduleParameters: DokkaModuleParametersKxs,

    outputDir: File,
    cacheRootDir: File?,
    offlineMode: Boolean,
    delayTemplateSubstitution: Boolean,
    pluginsClasspath: List<File>,

    componentsDir: File,
    componentsSpec: DokkaModuleComponentsSpec,
  ): DokkaConfiguration {
    val includes = componentsDir.resolve(componentsSpec.includesDirName).walk().drop(1).toSet()
    val sourceSetsDir = componentsDir.resolve(componentsSpec.sourceSetsDirName)

    return DokkaConfigurationImpl(
      moduleName = moduleParameters.moduleName,
      moduleVersion = moduleParameters.moduleVersion,
      outputDir = outputDir,
      cacheRoot = cacheRootDir,
      offlineMode = offlineMode,
      sourceSets = moduleParameters.sourceSets.map { it.convert(sourceSetsDir, componentsSpec) },
      pluginsClasspath = pluginsClasspath,
      pluginsConfiguration = moduleParameters.pluginsConfiguration.map { it.convert(componentsDir) },
      modules = emptyList(), // modules can't have other modules
      failOnWarning = moduleParameters.failOnWarning,
      delayTemplateSubstitution = delayTemplateSubstitution,
      suppressObviousFunctions = moduleParameters.suppressObviousFunctions,
      includes = includes,
      suppressInheritedMembers = moduleParameters.suppressInheritedMembers,
      finalizeCoroutines = moduleParameters.finalizeCoroutines,
    )
  }

  private fun DokkaSourceSetKxs.convert(
    componentsDir: File,
    componentsSpec: DokkaModuleComponentsSpec,
  ): DokkaSourceSetImpl {
    val sourceSetDir = componentsDir.resolve(sourceSetId)

    val classpath = sourceSetDir.resolve(componentsSpec.classpathDirName).walk().drop(1).toList()
    val samples = sourceSetDir.resolve(componentsSpec.samplesDirName).walk().drop(1).toSet()
    val includes = sourceSetDir.resolve(componentsSpec.includesDirName).walk().drop(1).toSet()
//      val sourceRootsDir = sourceSetDir.resolve(includesDirName).walk().drop(1).toSet()

    val sourceRoots = sourceRootsRelativePaths
      .map { componentsDir.resolve(it) }
      .toSet()
    val suppressedFiles = suppressedFileRelativePaths
      .map { componentsDir.resolve(it) }
      .toSet()

    val convertedSourceLinks = sourceLinks.mapToSet { link -> link.convert(sourceSetDir) }

    return DokkaSourceSetImpl(
      displayName = displayName,
      sourceSetID = sourceSetId.convert(),
      dependentSourceSets = dependentSourceSetIds.mapToSet { it.convert() },
      reportUndocumented = reportUndocumented,
      skipEmptyPackages = skipEmptyPackages,
      skipDeprecated = skipDeprecated,
      jdkVersion = jdkVersion,
      sourceLinks = convertedSourceLinks,
      perPackageOptions = perPackageOptions.map { it.convert() },
      externalDocumentationLinks =
      externalDocumentationLinks.mapToSet { it.convert() },
      languageVersion = languageVersion,
      apiVersion = apiVersion,
      noStdlibLink = !enableKotlinStdLibDocumentationLink,
      noJdkLink = !enableJdkDocumentationLink,
      analysisPlatform = analysisPlatform.dokkaType,
      documentedVisibilities = documentedVisibilities.mapToSet { it.dokkaType },

      classpath = classpath,
      sourceRoots = sourceRoots,
      samples = samples,
      includes = includes,
      suppressedFiles = suppressedFiles,
    )
  }


  private fun SourceLinkDefinitionKxs.convert(
    componentsDir: File,
  ): SourceLinkDefinitionImpl {
    val localDirectory = componentsDir.resolve(localDirectoryRelativePath)
    return SourceLinkDefinitionImpl(
      localDirectory = localDirectory.invariantSeparatorsPath,
      remoteUrl = remoteUrl.toURL(),
      remoteLineSuffix = remoteLineSuffix,
    )
  }

  private fun PackageOptionsKxs.convert() = PackageOptionsImpl(
    matchingRegex = matchingRegex,
    reportUndocumented = reportUndocumented,
    skipDeprecated = skipDeprecated,
    suppress = suppress,
    documentedVisibilities = documentedVisibilities.mapToSet { it.dokkaType },
    includeNonPublic = DokkaDefaults.includeNonPublic,
  )


  private fun PluginParametersKxs.convert(
    componentsDir: File,
  ): PluginConfigurationImpl {

    return PluginConfigurationImpl(
      fqPluginName = pluginFqn,
      serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
      values = values,
    )
  }


  private fun ExternalDocumentationLinkKxs.convert() =
    ExternalDocumentationLinkImpl(
      url = url.toURL(),
      packageListUrl = packageListUrl.toURL(),
    )


  private fun SourceSetIdKxs.convert() = DokkaSourceSetID(scopeId, sourceSetName)

  @DokkatooInternalApi
  companion object {
    private val logger: Logger = Logging.getLogger(DokkaParametersBuilder::class.java)
  }
}
