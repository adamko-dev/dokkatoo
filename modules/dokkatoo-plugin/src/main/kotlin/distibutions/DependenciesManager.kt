package dev.adamko.dokkatoo.distibutions

import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.*
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_BASE_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_MODULE_DESCRIPTION_NAME_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_PARAMETERS_TYPE_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_PARAMETER_FILE_TYPE_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKATOO_SOURCE_SET_ID_ATTRIBUTE
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.Companion.DOKKA_FORMAT_ATTRIBUTE
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.asConsumer
import dev.adamko.dokkatoo.internal.asProvider
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationVariant
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

@DokkatooInternalApi
class DependenciesManager(
  project: Project,
  private val d2Attributes: DokkatooConfigurationAttributes,
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
) {
  private val dependencyContainerNames = DependencyContainerNames(null)

  val dokkatooFilesProvider: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(dependencyContainerNames.dokkatoo) {
      description = "Provide Dokkatoo files to other subprojects"
      asProvider()
      attributes {
        attribute(DOKKATOO_BASE_ATTRIBUTE, d2Attributes.dokkatooBaseUsage)
      }
    }

  val dokkatooFilesConsumer: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(dependencyContainerNames.dokkatoo) {
      description = "Provide Dokkatoo files to other subprojects"
      asConsumer()
      attributes {
        attribute(DOKKATOO_BASE_ATTRIBUTE, d2Attributes.dokkatooBaseUsage)
      }
    }

  init {
    dokkatooFilesProvider.get().outgoing.variants
  }

//  private fun getDokkaParametersFiles(
//    named: String,
//    format: DokkaFormatAttribute,
//    property: DokkatooParameterFileTypeAttribute
//  ) {
//    dokkatooFilesConsumer.map { consumer ->
//      consumer
//        .incoming
//        .artifactView {
//          attributes {
//            attribute(DOKKATOO_PARAMETERS_KXS_ATTRIBUTE, objects.named(named))
//            attribute(DOKKATOO_PARAMETER_FILE_TYPE_ATTRIBUTE, property)
//            attribute(DOKKA_FORMAT_ATTRIBUTE, format)
//          }
//        }
//    }
//  }

  fun provideDokkaModuleDescriptionFiles(
    moduleName: Provider<DokkatooParameterAttribute.DokkaModuleDescriptionName>,
    format: DokkaFormatAttribute?,
    property: DokkatooParameterFileTypeAttribute,
    configure: ConfigurationVariant.() -> Unit,
  ): Provider<ConfigurationVariant> {
    return provideDokkaParametersFiles(
      parametersType = d2Attributes.parametersType.dokkaModuleDescription,
      moduleName = moduleName,
      format = format,
      property = property,
      configure = configure,
    )
  }

  fun consumeDokkaModuleDescriptionFiles(
    format: DokkaFormatAttribute,
    property: DokkatooParameterFileTypeAttribute
  ): Provider<Map<String?, FileCollection>> {
    return consumeDokkaParametersFiles(
      parametersType = d2Attributes.parametersType.dokkaModuleDescription,
      format = format,
      fileType = property
    ).groupingBy(DOKKATOO_MODULE_DESCRIPTION_NAME_ATTRIBUTE)
  }

  fun provideDokkaSourceSetFiles(
    moduleName: DokkatooParameterAttribute.DokkaModuleDescriptionName,
    format: DokkaFormatAttribute,
    property: DokkatooParameterFileTypeAttribute,
    configure: ConfigurationVariant.() -> Unit,
  ): Provider<ConfigurationVariant> {
    return provideDokkaParametersFiles(
      parametersType = d2Attributes.parametersType.dokkaSourceSet,
      moduleName = moduleName,
      format = format,
      property = property,
      configure = configure,
    )
  }

  fun consumeDokkaSourceSetFiles(
    format: DokkaFormatAttribute,
    property: DokkatooParameterFileTypeAttribute
  ): Provider<Map<String?, FileCollection>> {
    return consumeDokkaParametersFiles(
      parametersType = d2Attributes.parametersType.dokkaSourceSet,
      format = format,
      fileType = property
    ).groupingBy(DOKKATOO_SOURCE_SET_ID_ATTRIBUTE)
  }

  private fun provideDokkaParametersFiles(
    parametersType: DokkatooParameterAttribute.DokkaParametersType,
    moduleName: DokkatooParameterAttribute.DokkaModuleDescriptionName,
    format: DokkaFormatAttribute?,
    property: DokkatooParameterFileTypeAttribute,
    configure: ConfigurationVariant.() -> Unit,
  ): Provider<ConfigurationVariant> =
    provideDokkaParametersFiles(
      parametersType = parametersType,
      moduleName = providers.provider { moduleName },
      format = format,
      property = property,
      configure = configure,
    )

  private fun provideDokkaParametersFiles(
    parametersType: DokkatooParameterAttribute.DokkaParametersType,
    moduleName: Provider<DokkatooParameterAttribute.DokkaModuleDescriptionName>,
    format: DokkaFormatAttribute?,
    property: DokkatooParameterFileTypeAttribute,
    configure: ConfigurationVariant.() -> Unit,
  ): Provider<ConfigurationVariant> {
    return dokkatooFilesProvider.flatMap {
      it.outgoing.variants.register(
        "dokkatooParameters_${parametersType.name}_${format?.name}_${property.name}_${moduleName.get().name}"
      ) {
        withAttributes(
          parametersType = parametersType,
          moduleName = moduleName.get(),
          format = format,
          fileType = property
        )
        configure()
      }
    }
  }

  private fun consumeDokkaParametersFiles(
    parametersType: DokkatooParameterAttribute.DokkaParametersType,
    format: DokkaFormatAttribute,
    fileType: DokkatooParameterFileTypeAttribute
  ): Provider<Set<ResolvedArtifactResult>> {
    return dokkatooFilesConsumer.map { consumer ->
      consumer
        .incoming
        .artifactView {
          lenient(true)
          withAttributes(
            parametersType = parametersType,
            format = format,
            fileType = fileType,
            moduleName = null,
          )
        }.artifacts.artifacts
    }
  }

  private fun <T : Named> Provider<Set<ResolvedArtifactResult>>.groupingBy(
    attribute: Attribute<T>
  ): Provider<Map<String?, FileCollection>> {
    return map { artifacts ->
      artifacts
        .groupingBy { artifact ->
          artifact.variant.attributes.getAttribute(attribute)?.name
        }.fold(objects.fileCollection()) { files, element ->
          files.from(element.file)
        }
    }
  }

  private fun <T> HasConfigurableAttributes<T>.withAttributes(
    parametersType: DokkatooParameterAttribute.DokkaParametersType,
    moduleName: DokkatooParameterAttribute.DokkaModuleDescriptionName?,
    format: DokkaFormatAttribute?,
    fileType: DokkatooParameterFileTypeAttribute,
  ) {
    attributes {
      attribute(DOKKATOO_PARAMETERS_TYPE_ATTRIBUTE, parametersType)
      if (moduleName != null) {
        attribute(DOKKATOO_MODULE_DESCRIPTION_NAME_ATTRIBUTE, moduleName)
      }
      attribute(DOKKATOO_PARAMETER_FILE_TYPE_ATTRIBUTE, fileType)
      if (format != null) {
        attribute(DOKKA_FORMAT_ATTRIBUTE, format)
      }
    }
  }
}
