package dev.adamko.dokkatoo.dokka.parameters.builders


import dev.adamko.dokkatoo.dokka.parameters.*
import dev.adamko.dokkatoo.dokka.parameters.KotlinPlatform.Companion.dokkaType
import dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier.Companion.dokkaType
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.mapNotNullToSet
import dev.adamko.dokkatoo.internal.mapToSet
import org.jetbrains.dokka.*


/**
 * Convert the Gradle-focused [DokkaSourceSetSpec] into a [DokkaSourceSetImpl] instance, which
 * will be passed to Dokka Generator.
 *
 * The conversion is defined in a separate class to try and prevent classes from Dokka Generator
 * leaking into the public API.
 */
@DokkatooInternalApi
internal object DokkaSourceSetBuilder {

  fun buildAll(sourceSets: List<DokkaSourceSetSpec>): List<DokkaSourceSetImpl> =
    sourceSets.map(::build)

  private fun build(
    spec: DokkaSourceSetSpec,
  ): DokkaSourceSetImpl {
    return DokkaSourceSetImpl(
      // properties
      analysisPlatform = spec.analysisPlatform.get().dokkaType,
      apiVersion = spec.apiVersion.orNull,
      dependentSourceSets = spec.dependentSourceSets.mapToSet(::build),
      displayName = spec.displayName.get(),
      documentedVisibilities = spec.documentedVisibilities.get().mapToSet { it.dokkaType },
      externalDocumentationLinks = spec.externalDocumentationLinks.mapNotNullToSet(::build),
      jdkVersion = spec.jdkVersion.get(),
      languageVersion = spec.languageVersion.orNull,
      noJdkLink = !spec.enableJdkDocumentationLink.get(),
      noStdlibLink = !spec.enableKotlinStdLibDocumentationLink.get(),
      perPackageOptions = spec.perPackageOptions.map(::build),
      reportUndocumented = spec.reportUndocumented.get(),
      skipDeprecated = spec.skipDeprecated.get(),
      skipEmptyPackages = spec.skipEmptyPackages.get(),
      sourceLinks = spec.sourceLinks.mapToSet { build(it) },
      sourceSetID = build(spec.sourceSetId.get()),

      // files
      classpath = spec.classpath.files.toList(),
      includes = spec.includes.files,
      samples = spec.samples.files,
      sourceRoots = spec.sourceRoots.files,
      suppressedFiles = spec.suppressedFiles.files,
    )
  }

  private fun build(spec: DokkaExternalDocumentationLinkSpec): ExternalDocumentationLinkImpl? {
    if (!spec.enabled.getOrElse(true)) return null

    return ExternalDocumentationLinkImpl(
      url = spec.url.get().toURL(),
      packageListUrl = spec.packageListUrl.get().toURL(),
    )
  }

  private fun build(spec: DokkaPackageOptionsSpec): PackageOptionsImpl =
    PackageOptionsImpl(
      matchingRegex = spec.matchingRegex.get(),
      documentedVisibilities = spec.documentedVisibilities.get().mapToSet { it.dokkaType },
      reportUndocumented = spec.reportUndocumented.get(),
      skipDeprecated = spec.skipDeprecated.get(),
      suppress = spec.suppress.get(),
      includeNonPublic = DokkaDefaults.includeNonPublic,
    )

  private fun build(spec: DokkaSourceSetIdSpec): DokkaSourceSetID =
    DokkaSourceSetID(
      scopeId = spec.scopeId,
      sourceSetName = spec.sourceSetName
    )

  private fun build(spec: DokkaSourceLinkSpec): SourceLinkDefinitionImpl =
    SourceLinkDefinitionImpl(
      localDirectory = spec.localDirectory.asFile.get().invariantSeparatorsPath,
      remoteUrl = spec.remoteUrl.get().toURL(),
      remoteLineSuffix = spec.remoteLineSuffix.orNull,
    )
}
