package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.File
import java.nio.file.Paths
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
data class DokkaModuleDescriptionKxs(
  /** @see DokkaConfiguration.DokkaModuleDescription.name */
  val name: String,
  /** @see [org.gradle.api.Project.getPath] */
  val modulePath: String,
) {
//  internal fun convert(
//    moduleOutputDirectory: File,
//    moduleIncludes: Set<File>,
//  ): DokkaModuleDescriptionImpl =
//    DokkaModuleDescriptionImpl(
//      name = name,
//      sourceOutputDirectory = moduleOutputDirectory,
//      includes = moduleIncludes,
//      relativePathToOutputDirectory = File(modulePath.removePrefix(":").replace(':', '/')),
//    )
}
