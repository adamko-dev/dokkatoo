@file:UseSerializers(
  FileAsPathStringSerializer::class,
  URISerializer::class,
)

package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.mapToSet
import java.io.File
import java.net.URI
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
import org.jetbrains.dokka.*


// Implementations of DokkaConfiguration interfaces that can be serialized to files.
// Serialization is required because Gradle tasks can only pass data to one-another via files.


@Serializable
@DokkatooInternalApi
data class DokkaParametersKxs(
  val moduleName: String,
  val moduleVersion: String? = null,

  val offlineMode: Boolean,
  val failOnWarning: Boolean,
  val sourceSets: List<DokkaSourceSetKxs>,

  val pluginsConfiguration: List<PluginConfigurationKxs>,
//  val delayTemplateSubstitution: Boolean,
  val suppressObviousFunctions: Boolean,

  val suppressInheritedMembers: Boolean,
  val finalizeCoroutines: Boolean,

  val modules: List<DokkaModuleDescriptionKxs>,
) : Named {

  data class Files(
    val outputDir: File,
    val cacheRoot: File? = null,
    val pluginsClasspath: List<File>,
    val includes: Set<File>,
  )

  override fun getName(): String = moduleName

  fun convert(
    delayTemplateSubstitution: Boolean,
    files: Files,
    sourceSetFiles: Map<SourceSetIdKxs, DokkaSourceSetKxs.Files>,
    moduleFiles: Map<String, DokkaModuleDescriptionKxs.Files>,
  ): DokkaConfiguration {
    return DokkaConfigurationImpl(
      moduleName = moduleName,
      moduleVersion = moduleVersion,
      outputDir = files.outputDir,
      cacheRoot = files.cacheRoot,
      offlineMode = offlineMode,
      sourceSets = sourceSets.map {
        it.convert(sourceSetFiles[it.sourceSetId] ?: DokkaSourceSetKxs.Files.EMPTY)
      },
      pluginsClasspath = files.pluginsClasspath,
      pluginsConfiguration = pluginsConfiguration.map(PluginConfigurationKxs::convert),
      modules = modules.map {
        it.convert(moduleFiles[it.name] ?: error("missing files for $it"))
      },
      failOnWarning = failOnWarning,
      delayTemplateSubstitution = delayTemplateSubstitution,
      suppressObviousFunctions = suppressObviousFunctions,
      includes = files.includes,
      suppressInheritedMembers = suppressInheritedMembers,
      finalizeCoroutines = finalizeCoroutines,
    )
  }

  @Serializable
  @DokkatooInternalApi
  data class DokkaSourceSetKxs(
    val sourceSetId: SourceSetIdKxs,
    val displayName: String,
    val dependentSourceSetIds: Set<SourceSetIdKxs>,
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
    val analysisPlatform: Platform,
    val documentedVisibilities: Set<DokkaConfiguration.Visibility>,
  ) : Named {

    data class Files(
      val classpath: List<File>,
      val sourceRoots: Set<File>,
      val includes: Set<File>,
      val samples: Set<File>,
      val suppressedFiles: Set<File>,
    ) {
      companion object {
        val EMPTY = Files(
          classpath = emptyList(),
          sourceRoots = emptySet(),
          includes = emptySet(),
          samples = emptySet(),
          suppressedFiles = emptySet(),
        )
      }
    }

    override fun getName(): String = displayName

    internal fun convert(
      files: Files,
    ) =
      DokkaSourceSetImpl(
        displayName = displayName,
        sourceSetID = sourceSetId.convert(),
        classpath = files.classpath,
        sourceRoots = files.sourceRoots,
        dependentSourceSets = dependentSourceSetIds.mapToSet(SourceSetIdKxs::convert),
        samples = files.samples,
        includes = files.includes,
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
        suppressedFiles = files.suppressedFiles,
        analysisPlatform = analysisPlatform,
        documentedVisibilities = documentedVisibilities,
      )
  }


  @Serializable
  @DokkatooInternalApi
  data class SourceLinkDefinitionKxs(
    val localDirectory: String,
    val remoteUrl: URI,
    val remoteLineSuffix: String? = null,
  ) {

    internal fun convert() =
      SourceLinkDefinitionImpl(
        localDirectory = localDirectory,
        remoteUrl = remoteUrl.toURL(),
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
   */
  @Serializable
  @DokkatooInternalApi
  data class DokkaModuleDescriptionKxs(
    /** @see DokkaConfiguration.DokkaModuleDescription.name */
    val name: String,

    /** @see [org.gradle.api.Project.getPath] */
    val modulePath: String,
  ) {

    data class Files(
      /**
       * Location of the Dokka Module directory for a subproject.
       *
       * @see DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory
       */
      val sourceOutputDirectory: File,
      /** @see DokkaConfiguration.DokkaModuleDescription.includes */
      val includes: Set<File>,
    )

    internal fun convert(
      files: Files,
    ) =
      DokkaModuleDescriptionImpl(
        name = name,
        relativePathToOutputDirectory = File(modulePath.removePrefix(":").replace(':', '/')),
        includes = files.includes,
        sourceOutputDirectory = files.sourceOutputDirectory,
      )
  }


  @Serializable
  @DokkatooInternalApi
  data class ExternalDocumentationLinkKxs(
    val url: URI,
    val packageListUrl: URI,
  ) {
    internal fun convert() =
      ExternalDocumentationLinkImpl(
        url = url.toURL(),
        packageListUrl = packageListUrl.toURL(),
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


/** Serialize a [URI] as string */
private object URISerializer : KSerializer<URI> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("java.net.URI", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): URI =
    URI(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: URI): Unit =
    encoder.encodeString(value.toString())
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
