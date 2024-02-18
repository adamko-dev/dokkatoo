package dev.adamko.dokkatoo.dokka.plugins

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.addAll
import dev.adamko.dokkatoo.internal.putIfNotNull
import java.io.File
import javax.inject.Inject
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.*


/**
 * Configuration for Dokka's base HTML format
 *
 * [More information is available in the Dokka docs.](https://kotlinlang.org/docs/dokka-html.html#configuration)
 */
abstract class DokkaHtmlPluginParameters
@DokkatooInternalApi
@Inject
constructor(
  name: String,
  private val objects: ObjectFactory,
) : DokkaPluginParametersBaseSpec(
  name,
  DOKKA_HTML_PLUGIN_FQN,
) {

  /**
   * List of paths for image assets to be bundled with documentation.
   * The image assets can have any file extension.
   *
   * For more information, see
   * [Customizing assets](https://kotlinlang.org/docs/dokka-html.html#customize-assets).
   *
   * Be aware that files will be copied as-is to a specific directory inside the assembled Dokka
   * publication. This means that any relative paths must be written in such a way that they will
   * work _after_ the files are moved into the publication.
   *
   * It's best to try and mirror Dokka's directory structure in the source files, which can help
   * IDE inspections.
   */
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val customAssets: ConfigurableFileCollection

  /**
   * List of paths for `.css` stylesheets to be bundled with documentation and used for rendering.
   *
   * For more information, see
   * [Customizing assets](https://kotlinlang.org/docs/dokka-html.html#customize-assets).
   *
   * Be aware that files will be copied as-is to a specific directory inside the assembled Dokka
   * publication. This means that any relative paths must be written in such a way that they will
   * work _after_ the files are moved into the publication.
   *
   * It's best to try and mirror Dokka's directory structure in the source files, which can help
   * IDE inspections.
   */
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val customStyleSheets: ConfigurableFileCollection

  /**
   * This is a boolean option. If set to `true`, Dokka renders properties/functions and inherited
   * properties/inherited functions separately.
   *
   * This is disabled by default.
   */
  @get:Input
  @get:Optional
  abstract val separateInheritedMembers: Property<Boolean>

  /**
   * This is a boolean option. If set to `true`, Dokka merges declarations that are not declared as
   * [expect/actual](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html), but have the
   * same fully qualified name. This can be useful for legacy codebases.
   *
   * This is disabled by default.
   */
  @get:Input
  @get:Optional
  abstract val mergeImplicitExpectActualDeclarations: Property<Boolean>

  /** The text displayed in the footer. */
  @get:Input
  @get:Optional
  abstract val footerMessage: Property<String>

  /**
   * Path to the directory containing custom HTML templates.
   *
   * For more information, see [Templates](https://kotlinlang.org/docs/dokka-html.html#templates).
   */
  @get:InputDirectory
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val templatesDir: DirectoryProperty

  @DokkatooInternalApi
  class Serializer(
    private val objects: ObjectFactory,
    private val componentsDir: File,
  ) : KSerializer<DokkaHtmlPluginParameters> {
    @Serializable
    data class Delegate(
      val name: String,
      val customAssetsRelativePaths: List<String>,
      val customStyleSheetsRelativePaths: List<String>,
      val templatesDirRelativePath: String?,
      val mergeImplicitExpectActualDeclarations: Boolean?,
      val separateInheritedMembers: Boolean?,
      val footerMessage: String?,
    )

    private val serializer = Delegate.serializer()

    override val descriptor: SerialDescriptor
      get() = serializer.descriptor

    override fun deserialize(decoder: Decoder): DokkaHtmlPluginParameters {
      val delegate = serializer.deserialize(decoder)
      return objects.newInstance<DokkaHtmlPluginParameters>(delegate.name).apply {
        customAssets.from(
          delegate.customAssetsRelativePaths.map { componentsDir.resolve(it) }
        )
        customStyleSheets.from(
          delegate.customAssetsRelativePaths.map { componentsDir.resolve(it) }
        )
        if (delegate.templatesDirRelativePath != null) {
          templatesDir.set(componentsDir.resolve(delegate.templatesDirRelativePath))
        }
        separateInheritedMembers.set(delegate.separateInheritedMembers)
        footerMessage.set(delegate.footerMessage)
      }
    }

    override fun serialize(encoder: Encoder, value: DokkaHtmlPluginParameters) {
      Delegate(
        name = value.name,
        customStyleSheetsRelativePaths = value.customStyleSheets.asFileTree.files.map {
          it.relativeTo(componentsDir).invariantSeparatorsPath
        },
        customAssetsRelativePaths = value.customAssets.asFileTree.files.map {
          it.relativeTo(componentsDir).invariantSeparatorsPath
        },
        templatesDirRelativePath = value.templatesDir.asFile.orNull
          ?.relativeTo(componentsDir)?.invariantSeparatorsPath,
        footerMessage = value.footerMessage.orNull,
        separateInheritedMembers = value.separateInheritedMembers.orNull,
        mergeImplicitExpectActualDeclarations = value.mergeImplicitExpectActualDeclarations.orNull,
      )
    }
  }

  override fun serializer(
    componentsDir: File,
  ): KSerializer<String> {
    return object: KSerializer<String> {
      override val descriptor: SerialDescriptor = String.serializer().descriptor
      override fun deserialize(decoder: Decoder): String {
        String.serializer().deserialize(decoder)
      }
    }
    return Serializer(objects, componentsDir)
  }

//  override fun jsonEncode(
//    componentsDir: File,
//  ): String =
//    buildJsonObject {
//      putJsonArray("customAssets") {
//        addAll(customAssets.files)
//      }
//      putJsonArray("customStyleSheets") {
//        addAll(customStyleSheets.files)
//      }
//      putIfNotNull("separateInheritedMembers", separateInheritedMembers.orNull)
//      putIfNotNull(
//        "mergeImplicitExpectActualDeclarations",
//        mergeImplicitExpectActualDeclarations.orNull
//      )
//      putIfNotNull("footerMessage", footerMessage.orNull)
//      putIfNotNull(
//        "templatesDir",
//        templatesDir.orNull
//          ?.asFile
//          ?.relativeTo(componentsDir)
//          ?.invariantSeparatorsPath
//      )
//    }.toString()

  companion object {
    const val DOKKA_HTML_PARAMETERS_NAME = "html"
    const val DOKKA_HTML_PLUGIN_FQN = "org.jetbrains.dokka.base.DokkaBase"
  }
}
