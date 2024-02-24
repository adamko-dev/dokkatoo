@file:UseSerializers(
  URISerializer::class,
)

package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.dokka.plugins.DokkaPluginParametersBaseSpec
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.File
import java.net.URI
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.thauvin.erik.urlencoder.UrlEncoderUtil
import org.jetbrains.dokka.*

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
  /** name of the sibling directory that contains the module output */
  val moduleOutputDirName: String = "module",
  /** name of the sibling directory that contains the module includes */
  val moduleIncludesDirName: String = "includes",
)


@Serializable
@DokkatooInternalApi
data class DokkaModuleParametersKxs(
  val moduleName: String,
  val moduleVersion: String?,
  val failOnWarning: Boolean,
  val sourceSets: List<DokkaSourceSetKxs>,
  val pluginsConfiguration: List<PluginParametersKxs>,
  val suppressObviousFunctions: Boolean,
  val suppressInheritedMembers: Boolean,
  val finalizeCoroutines: Boolean,
) {

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
    val languageVersion: String?,
    val apiVersion: String?,
    val enableKotlinStdLibDocumentationLink: Boolean,
    val enableJdkDocumentationLink: Boolean,
    val analysisPlatform: KotlinPlatform,
    val documentedVisibilities: Set<VisibilityModifier>,
//    val sourceRootsDirName: String = "sourceRoots",
//    /** Name of sibling directory with source roots. Each directory within must be a copy of a source root. */
//    val sourceRootsDirName: String = "sourceRoots",
    /** DSS are placed into `$componentsDir`. Source root paths are relative to that. */
    val sourceRootsRelativePaths: List<String>,
    /** DSS are placed into `$componentsDir`. Suppressed paths are relative to that. */
    val suppressedFileRelativePaths: Set<String>,
//    val classpath: List<File>,/
//    val sourceRoots: Set<File>,
//    val samples: Set<File>,
//    val includes: Set<File>,
//    val suppressedFiles: Set<File>,
  )


  @Serializable
  @DokkatooInternalApi
  data class SourceLinkDefinitionKxs(
    val localDirectoryRelativePath: String,
    val remoteUrl: URI,
    val remoteLineSuffix: String? = null,
  ) {

    constructor(
      spec: DokkaSourceLinkSpec,
      componentsDir: File,
    ) : this(
      localDirectoryRelativePath = spec.localDirectory.get().asFile
        .relativeTo(componentsDir)
        .invariantSeparatorsPath,
      remoteUrl = spec.remoteUrl.get(),
      remoteLineSuffix = spec.remoteLineSuffix.orNull,
    )

  }


  @Serializable
  @DokkatooInternalApi
  data class PackageOptionsKxs(
    val matchingRegex: String,
    val reportUndocumented: Boolean? = null,
    val skipDeprecated: Boolean,
    val suppress: Boolean,
    val documentedVisibilities: Set<VisibilityModifier>,
  ) {
    constructor(spec: DokkaPackageOptionsSpec) : this(
      matchingRegex = spec.matchingRegex.get(),
      reportUndocumented = spec.reportUndocumented.orNull,
      skipDeprecated = spec.skipDeprecated.get(),
      suppress = spec.suppress.get(),
      documentedVisibilities = spec.documentedVisibilities.get(),
    )
  }


  @Serializable
  @DokkatooInternalApi
  data class PluginParametersKxs(
    val pluginFqn: String,
//    val serializationFormat: DokkaConfiguration.SerializationFormat,
    /** plugin configuration encoded as a JSON string */
    val values: String,
  ) {
    constructor(
      spec: DokkaPluginParametersBaseSpec,
      componentsDir: File,
    ) : this(
      pluginFqn = spec.pluginFqn,
      values = spec.jsonEncode(),
    )
  }


  @Serializable
  @DokkatooInternalApi
  data class ExternalDocumentationLinkKxs(
    val url: URI,
    val packageListUrl: URI,
  ) {
    constructor(spec: DokkaExternalDocumentationLinkSpec) : this(
      url = spec.url.get(),
      packageListUrl = spec.packageListUrl.get(),
    )

  }


  /**
   * Unique identifier of the scope that this source set is placed in.
   * Each scope provide only unique source set names.
   *
   * @see org.jetbrains.dokka.DokkaSourceSetID
   */
  @Serializable
  @DokkatooInternalApi
  data class SourceSetIdKxs(
    val scopeId: String,
    val sourceSetName: String,
  ) {
    constructor(spec: DokkaSourceSetIdSpec) : this(spec.scopeId, spec.sourceSetName)

    val key: String = "$scopeId/$sourceSetName"

    companion object {
      internal fun File.resolve(sourceSetId: SourceSetIdKxs): File =
        resolve(UrlEncoderUtil.encode(sourceSetId.key))
    }
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
