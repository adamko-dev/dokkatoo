package dev.adamko.dokkatoo.dependencies

import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooFormatAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooModuleComponentAttribute
//import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooModuleNameAttribute
//import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooModulePathAttribute
import dev.adamko.dokkatoo.internal.*
import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider


@DokkatooInternalApi
class ModuleComponentDependencies(
  project: Project,
  private val component: DokkatooAttribute.ModuleComponent,
  private val baseAttributes: BaseAttributes,
  private val formatAttributes: FormatAttributes,
  declaredDependencies: Configuration,
  baseConfigurationName: String,
) {
  private val formatName: String get() = formatAttributes.format.name
  private val componentName: String get() = component.name

  private val resolver: Configuration =
    project.configurations.create("${baseConfigurationName}${componentName}Resolver") {
      description = "Resolves Dokkatoo $formatName $componentName files."
      resolvable()
      extendsFrom(declaredDependencies)
      attributes {
        attribute(USAGE_ATTRIBUTE, baseAttributes.dokkatooUsage)
        attribute(DokkatooFormatAttribute, formatAttributes.format)
        attribute(DokkatooModuleComponentAttribute, component)
      }
    }

  val outgoing: Configuration =
    project.configurations.create("${baseConfigurationName}${componentName}Consumable") {
      description =
        "Provides Dokkatoo $formatName $componentName files for consumption by other subprojects."
      consumable()
      attributes {
        attribute(USAGE_ATTRIBUTE, baseAttributes.dokkatooUsage)
        attribute(DokkatooFormatAttribute, formatAttributes.format)
        attribute(DokkatooModuleComponentAttribute, component)
      }
    }

  /**
   * Get all files from declared dependencies.
   *
   * The artifacts will be filtered to ensure:
   *
   * - [DokkatooModuleComponentAttribute] equals [component]
   * - [DokkatooFormatAttribute] equals [FormatAttributes.format]
   *
   * This filtering should prevent a Gradle bug where it fetches random files.
   * Unfortunately, [org.gradle.api.artifacts.ArtifactView.ViewConfiguration.lenient] must be
   * enabled, which might obscure errors.
   */
  val incomingArtifactFiles: Provider<List<File>> =
    resolver.incomingArtifacts().map { it.map(ResolvedArtifactResult::getFile) }

  private fun Configuration.incomingArtifacts(): Provider<List<ResolvedArtifactResult>> {

    // Redefine variables locally, because Configuration Cache is easily confused
    // and produces confusing error messages.
    val baseAttributes = baseAttributes
    val formatAttributes = formatAttributes
    val incoming = incoming
    val incomingName = incoming.name
    val component = component

    return incoming
      .artifactView {
        @Suppress("UnstableApiUsage")
        withVariantReselection()
        attributes {
          attribute(USAGE_ATTRIBUTE, baseAttributes.dokkatooUsage)
          attribute(DokkatooFormatAttribute, formatAttributes.format)
          attribute(DokkatooModuleComponentAttribute, component)
        }
        lenient(true)
      }
      .artifacts
      .resolvedArtifacts
      .map { artifacts ->
        artifacts
          // Gradle says it will only use the attributes defined in the above
          // `artifactView {}`, but it doesn't, and the artifacts it finds might be
          // random ones with arbitrary attributes, so we have to filter again.
          .filter { artifact ->
            val variantAttributes = artifact.variant.attributes
            when {
              artifact.variant.attributes[USAGE_ATTRIBUTE]?.name != baseAttributes.dokkatooUsage.name -> {
                logger.info("[${incomingName}] ignoring artifact $artifact - USAGE_ATTRIBUTE != ${baseAttributes.dokkatooUsage} | attributes:${variantAttributes.toMap()}")
                false
              }

              variantAttributes[DokkatooFormatAttribute]?.name != formatAttributes.format.name        -> {
                logger.info("[${incomingName}] ignoring artifact $artifact - DokkatooFormatAttribute != ${formatAttributes.format} | attributes:${variantAttributes.toMap()}")
                false
              }

              variantAttributes[DokkatooModuleComponentAttribute]?.name != component.name             -> {
                logger.info("[${incomingName}] ignoring artifact $artifact - DokkatooModuleComponentAttribute != $component | attributes:${variantAttributes.toMap()}")
                false
              }

              else                                                                                    -> {
                logger.info("[${incomingName}] found valid artifact $artifact | attributes:${variantAttributes.toMap()}")
                true
              }
            }
          }
      }
  }

  @DokkatooInternalApi
  companion object {
    private val logger: Logger = Logging.getLogger(DokkatooAttribute.ModuleComponent::class.java)
  }
}
