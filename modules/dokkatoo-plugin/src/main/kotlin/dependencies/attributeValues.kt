package dev.adamko.dokkatoo.dependencies

import dev.adamko.dokkatoo.internal.Attribute
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.*


/** Common [Attribute] values for Dokkatoo [Configuration]s. */
@DokkatooInternalApi
class BaseAttributes(
  objects: ObjectFactory,
) {
  val dokkatooUsage: Usage = objects.named("dev.adamko.dokkatoo")
  val dokkaModuleComponents: DokkatooAttribute.Component = objects.named("dokka-module-components")
}


/** [Attribute] values for a specific Dokka format. */
@DokkatooInternalApi
class FormatAttributes(
  formatName: String,
  objects: ObjectFactory,
) {
  val format: DokkatooAttribute.Format = objects.named(formatName)

//  val moduleOutputDirectories: DokkatooAttribute.Component =
//    objects.named("ModuleOutputDirectories")
}
