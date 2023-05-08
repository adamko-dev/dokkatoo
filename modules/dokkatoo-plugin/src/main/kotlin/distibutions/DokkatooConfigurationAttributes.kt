package dev.adamko.dokkatoo.distibutions

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.*

/**
 * Gradle Configuration Attributes for sharing Dokkatoo files across subprojects.
 *
 * These attributes are used to tag [Configuration]s, so files can be shared between subprojects.
 */
@DokkatooInternalApi
abstract class DokkatooConfigurationAttributes
@Inject
constructor(
  private val objects: ObjectFactory,
) : ExtensionAware {

  /** A general attribute for all [Configuration]s that are used by the Dokka Gradle plugin */
  val dokkatooBaseUsage: DokkatooBaseAttribute = objects.named("dokkatoo")

//  val dokkaParameters: DokkatooCategoryAttribute = objects.named("generator-parameters")

//  val dokkaModuleFiles: DokkatooCategoryAttribute = objects.named("module-files")
//  val dokkaModuleSource: DokkatooCategoryAttribute = objects.named("module-source")

//  val dokkaGeneratorClasspath: DokkatooCategoryAttribute = objects.named("generator-classpath")
//
//  val dokkaPluginsClasspath: DokkatooCategoryAttribute = objects.named("plugins-classpath")


  @DokkatooInternalApi
  interface DokkatooBaseAttribute : Usage

  @DokkatooInternalApi
  interface DokkatooCategoryAttribute : Named

  @DokkatooInternalApi
  interface DokkaFormatAttribute : Named

  @DokkatooInternalApi
  enum class DokkaParametersType {
    /** for [Configuration]s that provide or consume Dokka parameter files */
    DokkaParameters,
    DokkaSourceSet,
    /** for [Configuration]s that provide or consume Dokka Module files */
    DokkaModuleDescription,
  }

  @DokkatooInternalApi
  interface DokkatooParameterIdAttribute : Named {
//    /** @see dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs */
//    interface DokkaParametersType : DokkatooParameterAttribute
    /** @see dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs.DokkaSourceSetKxs */
    interface DokkaSourceSetId : DokkatooParameterIdAttribute
    /** @see dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs.DokkaModuleDescriptionKxs */
    interface DokkaModuleDescriptionName : DokkatooParameterIdAttribute
  }

  @DokkatooInternalApi
  enum class DokkatooParameterFileTypeAttribute {
    //CacheRoot,
    PluginsClasspath,
    Classpath,
    SourceRoots,
    Samples,
    Includes,
    SuppressedFiles,
    SourceOutputDirectory,
  }

  @DokkatooInternalApi
  companion object {
    val DOKKATOO_BASE_ATTRIBUTE =
      Attribute<DokkatooBaseAttribute>("dev.adamko.dokkatoo.base")
    val DOKKATOO_CATEGORY_ATTRIBUTE =
      Attribute<DokkatooCategoryAttribute>("dev.adamko.dokkatoo.category")
    val DOKKA_FORMAT_ATTRIBUTE =
      Attribute<DokkaFormatAttribute>("dev.adamko.dokkatoo.format")

    val DOKKATOO_PARAMETERS_TYPE_ATTRIBUTE =
      Attribute<DokkaParametersType>("dev.adamko.dokkatoo.parameters_type")
    val DOKKATOO_SOURCE_SET_ID_ATTRIBUTE =
      Attribute<DokkatooParameterIdAttribute.DokkaSourceSetId>("dev.adamko.dokkatoo.source_set_id")
    val DOKKATOO_MODULE_DESCRIPTION_NAME_ATTRIBUTE =
      Attribute<DokkatooParameterIdAttribute.DokkaModuleDescriptionName>("dev.adamko.dokkatoo.module_description_name")
    val DOKKATOO_PARAMETER_FILE_TYPE_ATTRIBUTE =
      Attribute<DokkatooParameterFileTypeAttribute>("dev.adamko.dokkatoo.parameter_file_type")

    private inline fun <reified T> Attribute(name: String): Attribute<T> =
      Attribute.of(name, T::class.java)

//    private inline fun <reified T : Named> ObjectFactory.named()
//        : PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, T>> =
//      PropertyDelegateProvider { _, property ->
//        val attribute = named<T>(property.name)
//        ReadOnlyProperty { _, _ -> attribute }
//      }
  }
}
