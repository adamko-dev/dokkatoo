package dev.adamko.dokkatoo.distibutions

import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.DokkaFormatAttribute
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.DokkatooParameterAttribute.*
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes.DokkatooParameterFileTypeAttribute
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails


@DokkatooInternalApi
internal class DokkatooParameterFileTypeRule :
  AttributeCompatibilityRule<DokkatooParameterFileTypeAttribute> {
  override fun execute(details: CompatibilityCheckDetails<DokkatooParameterFileTypeAttribute>) {
    when {
      details.consumerValue == null || details.producerValue == null -> details.incompatible()
      details.consumerValue == details.producerValue                 -> details.compatible()
    }
  }
}


@DokkatooInternalApi
internal class DokkaFormatRule : AttributeCompatibilityRule<DokkaFormatAttribute> {
  override fun execute(details: CompatibilityCheckDetails<DokkaFormatAttribute>) {
    when {
      details.consumerValue == null                  -> details.incompatible()
      details.producerValue == null                  -> details.compatible()
      details.consumerValue == details.producerValue -> details.compatible()
    }
  }
}


@DokkatooInternalApi
internal class DokkaParametersTypeRule : AttributeCompatibilityRule<DokkaParametersType> {
  override fun execute(details: CompatibilityCheckDetails<DokkaParametersType>) {
    when {
      details.consumerValue == null || details.producerValue == null -> details.incompatible()
      details.consumerValue == details.producerValue                 -> details.compatible()
    }
  }
}


@DokkatooInternalApi
internal class DokkaSourceSetIdRule : AttributeCompatibilityRule<DokkaSourceSetId> {
  override fun execute(details: CompatibilityCheckDetails<DokkaSourceSetId>) {
    when {
      details.consumerValue == null || details.producerValue == null -> details.incompatible()
      details.consumerValue == details.producerValue                 -> details.compatible()
    }
  }
}


@DokkatooInternalApi
internal class DokkaModuleDescriptionNameRule : AttributeCompatibilityRule<DokkaModuleDescriptionName> {
  override fun execute(details: CompatibilityCheckDetails<DokkaModuleDescriptionName>) {
    when {
      details.consumerValue == null || details.producerValue == null -> details.incompatible()
      details.consumerValue == details.producerValue                 -> details.compatible()
    }
  }
}
