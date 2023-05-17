@file:UseSerializers(
  URISerializer::class,
  FileAsPathStringSerializer::class,
)

package dev.adamko.dokkatoo.dokka.parameters

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
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.Platform


// Implementations of DokkaConfiguration interfaces that can be serialized to files.
// Serialization is required because Gradle tasks can only pass data to one-another via files.


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
internal data class DokkaModuleDescriptionKxs(
  /** @see DokkaConfiguration.DokkaModuleDescription.name */
  val name: String,
  /** @see [org.gradle.api.Project.getPath] */
  val modulePath: String,

//  // TODO move files to parameter of convert(), and pass them between subprojects using
//  //      Configurations
//  /**
//   * Location of the Dokka Module directory for a subproject.
//   *
//   * @see DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory
//   */
//  val sourceOutputDirectory: File,
//  /** @see DokkaConfiguration.DokkaModuleDescription.includes */
//  val includes: Set<File>,
) {
//  internal fun convert() =
//    DokkaModuleDescriptionImpl(
//      name = name,
//      relativePathToOutputDirectory = File(modulePath.removePrefix(":").replace(':', '/')),
//      includes = includes,
//      sourceOutputDirectory = sourceOutputDirectory,
//    )
}

/**
 * Serializable DokkaSourceSet descriptor.
 *
 * This class is used to pass properties that describe a DokkaSourceSet between subprojects.
 *
 * File-based properties will not be serialized by this class as file-paths are different across
 * machines, which is not compatible with Gradle normalization, with the result that Gradle caching
 * is compromised. Instead, files must be passed between subprojects using Gradle Configurations.
 *
 * @see dev.adamko.dokkatoo.dokka.parameters.builders.DokkaSourceSetBuilder
 * @see org.jetbrains.dokka.DokkaSourceSetImpl
 */
@Serializable
@DokkatooInternalApi
internal data class DokkaSourceSetKxs(
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
) {

  /** @see org.jetbrains.dokka.SourceLinkDefinitionImpl */
  @Serializable
  @DokkatooInternalApi
  data class SourceLinkDefinitionKxs(
    val localDirectory: String,
    val remoteUrl: URI,
    val remoteLineSuffix: String? = null,
  )

  /** @see org.jetbrains.dokka.PackageOptionsImpl */
  @Serializable
  @DokkatooInternalApi
  data class PackageOptionsKxs(
    val matchingRegex: String,
    val reportUndocumented: Boolean? = null,
    val skipDeprecated: Boolean,
    val suppress: Boolean,
    val documentedVisibilities: Set<DokkaConfiguration.Visibility>,
  )

  /** @see org.jetbrains.dokka.ExternalDocumentationLinkImpl */
  @Serializable
  @DokkatooInternalApi
  data class ExternalDocumentationLinkKxs(
    val url: URI,
    val packageListUrl: URI,
  )

  /** @see org.jetbrains.dokka.DokkaSourceSetID */
  @Serializable
  @DokkatooInternalApi
  data class SourceSetIdKxs(
    val scopeId: String,
    val sourceSetName: String,
  )
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
    File(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: File): Unit =
    encoder.encodeString(value.invariantSeparatorsPath)
}
