package dev.adamko.dokkatoo.distributions

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware

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
) : ExtensionAware { // TODO make an object?

//  /** A general attribute for all [Configuration]s that are used by the Dokka Gradle plugin */
//  val dokkatooBaseUsage: DokkatooBaseAttribute = objects.named("dokkatoo")

//  /** for [Configuration]s that provide or consume Dokka parameter files */
//  val dokkaParameters: DokkatooCategoryAttribute = objects.named("generator-parameters")
//
//  /** for [Configuration]s that provide or consume Dokka Module files */
//  val dokkaModuleFiles: DokkatooCategoryAttribute = objects.named("module-files")
////  val dokkaModuleSource: DokkatooCategoryAttribute = objects.named("module-source")
//
//  val dokkaGeneratorClasspath: DokkatooCategoryAttribute = objects.named("generator-classpath")
//
//  val dokkaPluginsClasspath: DokkatooCategoryAttribute = objects.named("plugins-classpath")

  @DokkatooInternalApi
  enum class DokkatooComponentType : Usage {
    SourceSetFiles,
    ModuleFiles,
    PluginsClasspath,
    GeneratorClasspath,
    ;

    override fun getName(): String = name
  }

  /**
   * The format of a publication, e.g. HTML, Markdown, Jekyll GFM
   */
  @DokkatooInternalApi
  interface PublicationFormatAttribute : Named

  @DokkatooInternalApi
  interface DokkatooParametersIdAttribute : Named { // TODO make sealed in Gradle 8
    /**
     * Denotes a Configuration that contains files for a Dokka Source Set.
     *
     * The value should be the ID of the source set,
     * [dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec.sourceSetId].
     *
     * @see dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetKxs
     */
    interface DokkaSourceSetId : DokkatooParametersIdAttribute
    /**
     * Denotes a Configuration that contains files for a Dokka Module Description.
     *
     * The value should be the name of the Dokka module,
     * [dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec.moduleName].
     *
     * @see dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
     * @see dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
     */
    interface DokkaModuleDescriptionName : DokkatooParametersIdAttribute
  }

  @DokkatooInternalApi
  enum class DokkaComponentArtifactType : Named {
    ModuleDescriptorJson,
    SourceSetParametersJson,
    Classpath,
    Includes,
    PluginsClasspath,
    Samples,
    SourceOutputDirectory,
    SourceRoots,
    SuppressedFiles,
    ;

    override fun getName(): String = name
  }

  @DokkatooInternalApi
  companion object {
    val DOKKATOO_COMPONENT_ATTRIBUTE =
      Attribute<DokkatooComponentType>("dev.adamko.dokkatoo.component")

    val DOKKATOO_FORMAT_ATTRIBUTE =
      Attribute<PublicationFormatAttribute>("dev.adamko.dokkatoo.format")

//    val DOKKA_PARAMETER_ID_ATTRIBUTE =
//      Attribute<DokkatooParametersIdAttribute>("dev.adamko.dokkatoo.parameter_id")

    val DOKKA_SOURCE_SET_ID_ATTRIBUTE =
      Attribute<DokkatooParametersIdAttribute.DokkaSourceSetId>("dev.adamko.dokkatoo.source_set_id")

    val DOKKA_MODULE_DESCRIPTION_NAME_ATTRIBUTE =
      Attribute<DokkatooParametersIdAttribute.DokkaModuleDescriptionName>("dev.adamko.dokkatoo.module_description_name")

    val DOKKA_COMPONENT_ARTIFACT_ATTRIBUTE =
      Attribute<DokkaComponentArtifactType>("dev.adamko.dokkatoo.component_artifact")

    private inline fun <reified T> Attribute(name: String): Attribute<T> =
      Attribute.of(name, T::class.java)
  }
}
