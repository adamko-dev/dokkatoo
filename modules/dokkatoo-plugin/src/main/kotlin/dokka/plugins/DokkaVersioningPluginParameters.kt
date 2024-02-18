package dev.adamko.dokkatoo.dokka.plugins

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.File
import javax.inject.Inject
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.*


/**
 * Configuration for
 * [Dokka's Versioning plugin](https://github.com/Kotlin/dokka/tree/master/plugins/versioning#readme).
 *
 * The versioning plugin provides the ability to host documentation for multiple versions of your
 * library/application with seamless switching between them. This, in turn, provides a better
 * experience for your users.
 *
 * Note: The versioning plugin only works with Dokka's HTML format.
 */
abstract class DokkaVersioningPluginParameters
@DokkatooInternalApi
@Inject
constructor(
  name: String,
  private val objects: ObjectFactory,
) : DokkaPluginParametersBaseSpec(
  name,
  DOKKA_VERSIONING_PLUGIN_FQN,
) {

  /**
   * The version of your application/library that documentation is going to be generated for.
   * This will be the version shown in the dropdown menu.
   */
  @get:Input
  @get:Optional
  abstract val version: Property<String>

  /**
   * An optional list of strings that represents the order that versions should appear in the
   * dropdown menu.
   *
   * Must match [version] string exactly. The first item in the list is at the top of the dropdown.
   * Any versions not in this list will be excluded from the dropdown.
   *
   * If no versions are supplied the versions will be ordered using SemVer ordering.
   */
  @get:Input
  @get:Optional
  abstract val versionsOrdering: ListProperty<String>

  /**
   * An optional path to a parent folder that contains other documentation versions.
   * It requires a specific directory structure.
   *
   * For more information, see
   * [Directory structure](https://github.com/Kotlin/dokka/blob/master/plugins/versioning/README.md#directory-structure).
   */
  @get:InputDirectory
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val olderVersionsDir: DirectoryProperty

  /**
   * An optional list of paths to other documentation versions. It must point to Dokka's outputs
   * directly. This is useful if different versions can't all be in the same directory.
   */
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val olderVersions: ConfigurableFileCollection

  /**
   * An optional boolean value indicating whether to render the navigation dropdown on all pages.
   *
   * Set to `true` by default.
   */
  @get:Input
  @get:Optional
  abstract val renderVersionsNavigationOnAllPages: Property<Boolean>

//
//  @DokkatooInternalApi
//  class ValuesSerializer(
//    private val objects: ObjectFactory,
//    private val componentsDir: File,
//  ) : KSerializer<DokkaVersioningPluginParameters> {
//    @Serializable
//    data class Delegate(
////      val name: String,
//      val version: String?,
//      val olderVersionsDirRelativePath: String?,
//      val renderVersionsNavigationOnAllPages: Boolean?,
//      val versionsOrdering: List<String>?,
//      val olderVersionsRelativePaths: List<String>?,
//
////      val customStyleSheetsRelativePaths: List<String>,
////      val mergeImplicitExpectActualDeclarations: Boolean?,
//    )
//
//    private val serializer = Delegate.serializer()
//
//    override val descriptor: SerialDescriptor
//      get() = serializer.descriptor
//
//    override fun deserialize(decoder: Decoder): DokkaVersioningPluginParameters {
//      val delegate = serializer.deserialize(decoder)
//      return objects.newInstance<DokkaVersioningPluginParameters>(delegate.name).apply {
//        version.set(delegate.version)
//        renderVersionsNavigationOnAllPages.set(delegate.renderVersionsNavigationOnAllPages)
//        // only create versionsOrdering values are present, otherwise Dokka interprets
//        // an empty list as "no versions, show nothing".
//        versionsOrdering.set(delegate.versionsOrdering?.ifEmpty { null })
//        if (delegate.olderVersionsDirRelativePath != null) {
//          olderVersionsDir.fileValue(
//            componentsDir.resolve(delegate.olderVersionsDirRelativePath)
//          )
//        }
//      }
//    }
//
//    override fun serialize(encoder: Encoder, value: DokkaVersioningPluginParameters) {
//      Delegate(
//        name = value.name,
//        version = value.version.orNull,
//        olderVersionsRelativePaths = value.olderVersions.map {
//          it.relativeTo(componentsDir).invariantSeparatorsPath
//        },
//        olderVersionsDirRelativePath = value.olderVersionsDir.asFile.orNull
//          ?.relativeTo(componentsDir)
//          ?.invariantSeparatorsPath,
//        renderVersionsNavigationOnAllPages = value.renderVersionsNavigationOnAllPages.orNull,
//        versionsOrdering = value.versionsOrdering.orNull,
//      )
//    }
//  }
//
//  override fun valuesSerializer(
//    componentsDir: File,
//  ): KSerializer<DokkaVersioningPluginParameters> {
//    return ValuesSerializer(objects, componentsDir)
//  }
//

//  override fun jsonEncode(): String {
//    val versionsOrdering = versionsOrdering.orNull.orEmpty()
//
//    return buildJsonObject {
//      putIfNotNull("version", version.orNull)
//      if (versionsOrdering.isNotEmpty()) {
//        // only create versionsOrdering values are present, otherwise Dokka interprets
//        // an empty list as "no versions, show nothing".
//        putJsonArray("versionsOrdering") { addAll(versionsOrdering) }
//      }
//      putIfNotNull("olderVersionsDir", olderVersionsDir.orNull?.asFile)
//      putJsonArray("olderVersions") {
//        addAll(olderVersions.files)
//      }
//      putIfNotNull("renderVersionsNavigationOnAllPages", renderVersionsNavigationOnAllPages.orNull)
//    }.toString()
//  }

  companion object {
    const val DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME = "versioning"
    const val DOKKA_VERSIONING_PLUGIN_FQN = "org.jetbrains.dokka.versioning.VersioningPlugin"
  }
}
