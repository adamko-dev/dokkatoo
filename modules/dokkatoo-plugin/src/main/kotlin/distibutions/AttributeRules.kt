package dev.adamko.dokkatoo.distributions

import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.*
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkatooParametersIdAttribute.DokkaModuleDescriptionName
import dev.adamko.dokkatoo.distributions.DokkatooConfigurationAttributes.DokkatooParametersIdAttribute.DokkaSourceSetId
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.api.Named
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.attributes.MultipleCandidatesDetails


@DokkatooInternalApi
internal object DokkaParameterFileTypeRule : ExactNameMatchRule<DokkaComponentArtifactType>()

@DokkatooInternalApi
internal object DokkatooComponentTypeRule : ExactNameMatchRule<DokkatooComponentType>()

@DokkatooInternalApi
internal object DokkaFormatRule : ExactNameMatchRule<PublicationFormatAttribute>()

@DokkatooInternalApi
internal object DokkaSourceSetIdRule : ExactNameMatchRule<DokkaSourceSetId>()

@DokkatooInternalApi
internal object DokkaModuleDescriptionNameRule : ExactNameMatchRule<DokkaModuleDescriptionName>()


@DokkatooInternalApi
internal object DokkaParameterFileTypeDisambiguationRule
  : ExactMatchDisambiguationRule<DokkaComponentArtifactType>()

@DokkatooInternalApi
internal object DokkatooComponentTypeDisambiguationRule
  : ExactMatchDisambiguationRule<DokkatooComponentType>()

@DokkatooInternalApi
internal object DokkaFormatDisambiguationRule
  : ExactMatchDisambiguationRule<PublicationFormatAttribute>()

@DokkatooInternalApi
internal object DokkaSourceSetIdDisambiguationRule
  : ExactMatchDisambiguationRule<DokkaSourceSetId>()

@DokkatooInternalApi
internal object DokkaModuleDescriptionNameDisambiguationRule
  : ExactMatchDisambiguationRule<DokkaModuleDescriptionName>()


//region rule utils

/** Re-implemented version of [CompatibilityCheckDetails] to support Kotlin type-casting */
@DokkatooInternalApi
internal class CompatibilityCheckDetailsContext<T : Named>(
  private val details: CompatibilityCheckDetails<T>
) {
  val consumerValue: T? = details.consumerValue
  val producerValue: T? = details.producerValue

  fun incompatible(): Unit = details.incompatible()
  fun compatible(): Unit = details.compatible()
}

/** Re-implemented version of [AttributeCompatibilityRule] to support Kotlin type-casting */
@DokkatooInternalApi
internal abstract class AttributeCompatibilityRule2<T : Named>(
  private val check: CompatibilityCheckDetailsContext<T>.() -> Unit,
) : AttributeCompatibilityRule<T> {
  final override fun execute(details: CompatibilityCheckDetails<T>) {
    CompatibilityCheckDetailsContext(details).check()
  }
}

/** Consumer and producer name must be present and match exactly. */
@DokkatooInternalApi
internal abstract class ExactNameMatchRule<T : Named> : AttributeCompatibilityRule2<T>({
  when {
    consumerValue == null                    -> incompatible()
    producerValue == null                    -> incompatible()
    consumerValue.name == producerValue.name -> compatible()
  }
})

internal abstract class ExactMatchDisambiguationRule<T : Named> : AttributeDisambiguationRule<T> {
  override fun execute(details: MultipleCandidatesDetails<T>) {

    val consumerName = details.consumerValue?.name ?: return

    details.candidateValues
      .filterNotNull()
      .forEach { candidate ->
        if (candidate.name == consumerName) {
          details.closestMatch(candidate)
        }
      }
  }
}

//endregion
