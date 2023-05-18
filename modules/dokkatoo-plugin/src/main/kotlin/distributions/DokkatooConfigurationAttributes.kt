package dev.adamko.dokkatoo.distributions

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
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
  objects: ObjectFactory,
) {

  /** A general attribute for all [Configuration]s that are used by the Dokka Gradle plugin */
  val dokkatooBaseUsage: DokkatooBaseAttribute = objects.named("dokkatoo")

  /** for [Configuration]s that provide or consume Dokka parameter files */
  val dokkaParameters: DokkatooCategoryAttribute = objects.named("generator-parameters")

  /** for [Configuration]s that provide or consume Dokka Module files */
  val dokkaModuleDescriptors: DokkatooCategoryAttribute = objects.named("module-descriptors")
  val dokkaModuleIncludes: DokkatooCategoryAttribute = objects.named("module-includes")
  val dokkaModuleSourceOutputDirs: DokkatooCategoryAttribute =
    objects.named("module-source-output-dirs")
//  val dokkaModuleSource: DokkatooCategoryAttribute = objects.named("module-source")

  val dokkaGeneratorClasspath: DokkatooCategoryAttribute = objects.named("generator-classpath")

  val dokkaPluginsClasspath: DokkatooCategoryAttribute = objects.named("plugins-classpath")

  @DokkatooInternalApi
  interface DokkatooBaseAttribute : Usage

  @DokkatooInternalApi
  interface DokkatooCategoryAttribute : Named

  @DokkatooInternalApi
  interface DokkaFormatAttribute : Named

  @DokkatooInternalApi
  interface DokkatooParameterIdAttribute : Named {
    //    /** @see dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs */
//    interface DokkaParametersType : DokkatooParameterAttribute
//    /** @see dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs.DokkaSourceSetKxs */
    interface DokkaSourceSetId : DokkatooParameterIdAttribute
    //    /** @see dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs.DokkaModuleDescriptionKxs */
    interface DokkaModuleDescriptionName : DokkatooParameterIdAttribute
  }

  @DokkatooInternalApi
  companion object {
    val DOKKATOO_BASE_ATTRIBUTE =
      Attribute<DokkatooBaseAttribute>("dev.adamko.dokkatoo.base")
    val DOKKATOO_CATEGORY_ATTRIBUTE =
      Attribute<DokkatooCategoryAttribute>("dev.adamko.dokkatoo.category")
    val DOKKA_FORMAT_ATTRIBUTE =
      Attribute<DokkaFormatAttribute>("dev.adamko.dokkatoo.format")
    val DOKKATOO_SOURCE_SET_ID_ATTRIBUTE =
      Attribute<DokkatooParameterIdAttribute.DokkaSourceSetId>("dev.adamko.dokkatoo.source_set_id")
    val DOKKATOO_MODULE_DESCRIPTION_NAME_ATTRIBUTE =
      Attribute<DokkatooParameterIdAttribute.DokkaModuleDescriptionName>("dev.adamko.dokkatoo.module_description_name")

    private inline fun <reified T> Attribute(name: String): Attribute<T> =
      Attribute.of(name, T::class.java)
  }
}
