package dev.adamko.dokkatoo.distributions

import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.*
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKATOO_COMPONENT_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKATOO_FORMAT_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKA_MODULE_DESCRIPTION_NAME_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKA_COMPONENT_ARTIFACT_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKA_SOURCE_SET_ID_ATTRIBUTE
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkatooParametersIdAttribute.DokkaModuleDescriptionName
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkatooParametersIdAttribute.DokkaSourceSetId
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.joinCamelCase
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

@DokkatooInternalApi
class FormatDependenciesManager(
  private val format: PublicationFormatAttribute,
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
  private val baseDependencyContainers: BaseDependencyContainers
) {
  //region Consumers

  fun consumeDokkatooModuleFiles(
    fileType: DokkaComponentArtifactType,
//    moduleName: DokkaModuleDescriptionName? = null,
  ): Provider<FileCollection> {
    return baseDependencyContainers.dokkatooModuleConsumer
      .consumeDokkatooFiles(
        id = null,
        fileType = fileType,
        component = DokkatooComponentType.ModuleFiles,
      ).map { artifacts ->
        artifacts.fold(objects.fileCollection()) { acc, artifact ->
          acc.from(artifact.file)
        }
      }
  }

  fun consumeDokkatooModuleFilesGroupedByName(
    fileType: DokkaComponentArtifactType,
  ): Provider<Map<String?, FileCollection>> {
    return baseDependencyContainers.dokkatooModuleConsumer
      .consumeDokkatooFiles(
        id = null,
        fileType = fileType,
        component = DokkatooComponentType.ModuleFiles,
      ).groupingBy(DOKKA_MODULE_DESCRIPTION_NAME_ATTRIBUTE)
  }


  fun consumeDokkaSourceSetFilesGroupedById(
//    id: DokkaSourceSetId,
    fileType: DokkaComponentArtifactType,
  ): Provider<Map<String?, FileCollection>> {
    return baseDependencyContainers.dokkatooSourceSetsConsumer
      .consumeDokkatooFiles(
        component = DokkatooComponentType.SourceSetFiles,
        id = null,
        fileType = fileType,
      ).groupingBy(DOKKA_SOURCE_SET_ID_ATTRIBUTE)
  }

  private fun NamedDomainObjectProvider<Configuration>.consumeDokkatooFiles(
    component: DokkatooComponentType,
    id: DokkatooParametersIdAttribute?,
    fileType: DokkaComponentArtifactType,
  ): Provider<Set<ResolvedArtifactResult>> {
    return map { consumer ->
      consumer
        .incoming
        .artifactView {
          lenient(true)
          @Suppress("UnstableApiUsage")
          withVariantReselection() // TODO might need variant reselection?
          withAttributes(
            id = id,
            fileType = fileType,
            component = component,
          )
        }.artifacts.artifacts
    }
  }
  //endregion

  //region Providers

  fun provideDokkatooModuleArtifact(
    moduleName: Provider<DokkaModuleDescriptionName>,
    fileType: DokkaComponentArtifactType,
    artifactProvider: OutgoingArtifactProvider.Artifact,
  ): Unit =
    baseDependencyContainers.dokkatooComponentsProvider.provideDokkatooArtifact(
      component = DokkatooComponentType.ModuleFiles,
      id = moduleName,
      property = fileType,
      artifactProvider = artifactProvider,
    )

  fun provideDokkaSourceSetFile(
    id: DokkaSourceSetId,
    property: DokkaComponentArtifactType,
    artifactProvider: OutgoingArtifactProvider.Artifact,
  ): Unit =
    baseDependencyContainers.dokkatooComponentsProvider.provideDokkatooArtifact(
      component = DokkatooComponentType.SourceSetFiles,
      id = providers.provider { id },
      property = property,
      artifactProvider = artifactProvider,
    )

  private fun NamedDomainObjectProvider<Configuration>.provideDokkatooArtifact(
    component: DokkatooComponentType,
    id: DokkatooParametersIdAttribute,
    property: DokkaComponentArtifactType,
    artifactProvider: OutgoingArtifactProvider.Artifact,
  ): Unit =
    provideDokkatooArtifact(
      id = providers.provider { id },
      property = property,
      artifactProvider = artifactProvider,
      component = component,
    )

  private fun NamedDomainObjectProvider<Configuration>.provideDokkatooArtifact(
    component: DokkatooComponentType,
    id: Provider<out DokkatooParametersIdAttribute>,
    property: DokkaComponentArtifactType,
//    artifactProvider: OutgoingArtifactProvider<*>,
    artifactProvider: OutgoingArtifactProvider.Artifact,
  ) {
//    val configure = Action<ConfigurablePublishArtifact> {
//      withAttributes(
//        id = id.get(),
//        fileType = property,
//        component = component,
//      )
//    }

//    fun HasConfigurableAttributes<*>.attributes(): Unit =

    configure {
//      outgoing {
//        when (artifactProvider) {
//          is OutgoingArtifactProvider.Artifact  ->
//            artifact(artifactProvider(), configure)// { attributes() }
//
//          is OutgoingArtifactProvider.Artifacts ->
//            @Suppress("UnstableApiUsage")
//            artifacts(artifactProvider()) {
//              this.attr
//              attributes() }
//        }
//      }
      //
      outgoing.variants.maybeCreate(
        joinCamelCase("dokkatoo", component.name, format.name, property.name, id.get().name)
      ).apply {
        withAttributes(
          id = id.get(),
          fileType = property,
          component = component,
        )
        artifact(artifactProvider())
      }
    }
  }
  //endregion

  //region Utils
  private fun <T> HasConfigurableAttributes<T>.withAttributes(
    id: DokkatooParametersIdAttribute?,
    fileType: DokkaComponentArtifactType,
    component: DokkatooComponentType,
  ) {
    attributes {
      attribute(DOKKATOO_COMPONENT_ATTRIBUTE, component)
      attribute(DOKKATOO_FORMAT_ATTRIBUTE, format)
      attribute(DOKKA_COMPONENT_ARTIFACT_ATTRIBUTE, fileType)

      when (id) {
        is DokkaModuleDescriptionName -> attribute(DOKKA_MODULE_DESCRIPTION_NAME_ATTRIBUTE, id)
        is DokkaSourceSetId           -> attribute(DOKKA_SOURCE_SET_ID_ATTRIBUTE, id)
      }
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

  @DokkatooInternalApi
  interface OutgoingArtifactProvider<T> { // TODO make sealed in Gradle 8
    operator fun invoke(): T
    fun interface Artifact : OutgoingArtifactProvider<Any>
    fun interface Artifacts : OutgoingArtifactProvider<Provider<Iterable<Any>>>
  }
  //endregion
}
