@file:UseSerializers(
  FileAsPathStringSerializer::class,
  URLSerializer::class,
)

package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.mapToSet
import java.io.File
import java.net.URL
import java.nio.file.Paths
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.gradle.api.Named
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.*


// Implementations of DokkaConfiguration interfaces that can be serialized to files.
// Serialization is required because Gradle tasks can only pass data to one-another via files.


@Serializable
@DokkatooInternalApi
data class DokkaParametersKxs(
  val moduleName: String,
  val moduleVersion: String? = null,
  val cacheRoot: File? = null,
  val offlineMode: Boolean,
  val failOnWarning: Boolean,
  val sourceSets: List<DokkaSourceSetKxs>,
  val pluginsClasspath: List<File>,
  val pluginsConfiguration: List<PluginConfigurationKxs>,
  val delayTemplateSubstitution: Boolean,
  val suppressObviousFunctions: Boolean,
  val includes: Set<File>,
  val suppressInheritedMembers: Boolean,
  val finalizeCoroutines: Boolean,

  val modules: List<DokkaModuleDescriptionKxs>,
) : Named {

  override fun getName(): String = moduleName

  fun convert(outputDir: File): DokkaConfiguration {
    return DokkaConfigurationImpl(
      moduleName = moduleName,
      moduleVersion = moduleVersion,
      outputDir = outputDir,
      cacheRoot = cacheRoot,
      offlineMode = offlineMode,
      sourceSets = sourceSets.map(DokkaSourceSetKxs::convert),
      pluginsClasspath = pluginsClasspath,
      pluginsConfiguration = pluginsConfiguration.map(PluginConfigurationKxs::convert),
      modules = modules.map(DokkaModuleDescriptionKxs::convert),
      failOnWarning = failOnWarning,
      delayTemplateSubstitution = delayTemplateSubstitution,
      suppressObviousFunctions = suppressObviousFunctions,
      includes = includes,
      suppressInheritedMembers = suppressInheritedMembers,
      finalizeCoroutines = finalizeCoroutines,
    )
  }

  @Serializable
  @DokkatooInternalApi
  data class DokkaSourceSetKxs(
    val sourceSetId: SourceSetIdKxs,
    val displayName: String,
    val classpath: List<File>,
    val sourceRoots: Set<File>,
    val dependentSourceSetIds: Set<SourceSetIdKxs>,
    val samples: Set<File>,
    val includes: Set<File>,
    val reportUndocumented: Boolean,
    val skipEmptyPackages: Boolean,
    val skipDeprecated: Boolean,
    val jdkVersion: Int,
    val sourceLinks: Set<SourceLinkDefinitionKxs>,
    val perPackageOptions: List<PackageOptionsKxs>,
    val externalDocumentationLinks: Set<ExternalDocumentationLinkKxs>,
    val languageVersion: String? = null,
    val apiVersion: String? = null,
    val enableKotlinStdLibDocumentationLink: Boolean,
    val enableJdkDocumentationLink: Boolean,
    val suppressedFiles: Set<File>,
    val analysisPlatform: Platform,
    val documentedVisibilities: Set<DokkaConfiguration.Visibility>,
  ) : Named {

    override fun getName(): String = displayName

    internal fun convert() =
      DokkaSourceSetImpl(
        displayName = displayName,
        sourceSetID = sourceSetId.convert(),
        classpath = classpath,
        sourceRoots = sourceRoots,
        dependentSourceSets = dependentSourceSetIds.mapToSet(SourceSetIdKxs::convert),
        samples = samples,
        includes = includes,
        reportUndocumented = reportUndocumented,
        skipEmptyPackages = skipEmptyPackages,
        skipDeprecated = skipDeprecated,
        jdkVersion = jdkVersion,
        sourceLinks = sourceLinks.mapToSet(SourceLinkDefinitionKxs::convert),
        perPackageOptions = perPackageOptions.map(PackageOptionsKxs::convert),
        externalDocumentationLinks = externalDocumentationLinks.mapToSet(
          ExternalDocumentationLinkKxs::convert
        ),
        languageVersion = languageVersion,
        apiVersion = apiVersion,
        noStdlibLink = !enableKotlinStdLibDocumentationLink,
        noJdkLink = !enableJdkDocumentationLink,
        suppressedFiles = suppressedFiles,
        analysisPlatform = analysisPlatform,
        documentedVisibilities = documentedVisibilities,
      )
  }


  @Serializable
  @DokkatooInternalApi
  data class SourceLinkDefinitionKxs(
    val localDirectory: String,
    val remoteUrl: URL,
    val remoteLineSuffix: String? = null,
  ) {

    internal fun convert() =
      SourceLinkDefinitionImpl(
        localDirectory = localDirectory,
        remoteUrl = remoteUrl,
        remoteLineSuffix = remoteLineSuffix,
      )
  }


  @Serializable
  @DokkatooInternalApi
  data class PackageOptionsKxs(
    val matchingRegex: String,
    val reportUndocumented: Boolean? = null,
    val skipDeprecated: Boolean,
    val suppress: Boolean,
    val documentedVisibilities: Set<DokkaConfiguration.Visibility>,
  ) {
    internal fun convert() = PackageOptionsImpl(
      matchingRegex = matchingRegex,
      reportUndocumented = reportUndocumented,
      skipDeprecated = skipDeprecated,
      suppress = suppress,
      documentedVisibilities = documentedVisibilities,
      includeNonPublic = DokkaDefaults.includeNonPublic,
    )
  }


  @Serializable
  @DokkatooInternalApi
  data class PluginConfigurationKxs(
    val fqPluginName: String,
    val serializationFormat: DokkaConfiguration.SerializationFormat,
    /** plugin configuration encoded as a string that might contain escaped JSON/XML */
    val values: String,
  ) {
    fun convert() = PluginConfigurationImpl(
      fqPluginName = fqPluginName,
      serializationFormat = serializationFormat,
      values = values
    )
  }


  /**
   * Any subproject can be merged into a single Dokka Publication. To do this, first it must create
   * a Dokka Module. A [DokkaModuleDescriptionKxs] describes a config file for the Dokka Module that
   * describes its content. This config file will be used by any aggregating project to produce
   * a Dokka Publication with multiple modules.
   *
   * Note: this class implements [java.io.Serializable] because it is used as a
   * [Gradle Property][org.gradle.api.provider.Property], and Gradle must be able to fingerprint
   * property values classes using Java Serialization.
   *
   * All other configuration data classes also implement [java.io.Serializable] via their parent interfaces.
   */
  @Serializable
  @DokkatooInternalApi
  data class DokkaModuleDescriptionKxs(
    /** @see DokkaConfiguration.DokkaModuleDescription.name */
    val name: String,
    /**
     * Location of the Dokka Module directory for a subproject.
     *
     * @see DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory
     */
    val sourceOutputDirectory: File,
    /** @see DokkaConfiguration.DokkaModuleDescription.includes */
    val includes: Set<File>,

    /** @see [org.gradle.api.Project.getPath] */
    val modulePath: String,
  ) {

    internal fun convert() = DokkaModuleDescriptionImpl(
      name = name,
      relativePathToOutputDirectory = File(modulePath.removePrefix(":").replace(':', '/')),
      includes = includes,
      sourceOutputDirectory = sourceOutputDirectory,
    )
  }


  @Serializable
  @DokkatooInternalApi
  data class ExternalDocumentationLinkKxs(
    val url: URL,
    val packageListUrl: URL,
  ) {
    internal fun convert() =
      ExternalDocumentationLinkImpl(
        url = url,
        packageListUrl = packageListUrl,
      )
  }

  @Serializable
  @DokkatooInternalApi
  /**
   * @see org.jetbrains.dokka.DokkaSourceSetID
   */
  data class SourceSetIdKxs(
    val scopeId: String,
    val sourceSetName: String,
  ) {
    fun convert() = DokkaSourceSetID(scopeId, sourceSetName)
  }
}


/** Serialize a [URL] as string */
private object URLSerializer : KSerializer<URL> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("java.net.URL", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): URL = URL(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: URL) = encoder.encodeString(value.toString())
}


/**
 * Serialize a [File] as an absolute, canonical file path, with
 * [invariant path separators][invariantSeparatorsPath]
 */
private object FileAsPathStringSerializer : KSerializer<File> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("java.io.File", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): File =
    Paths.get(decoder.decodeString()).toFile()

  override fun serialize(encoder: Encoder, value: File): Unit =
    encoder.encodeString(value.invariantSeparatorsPath)
}
