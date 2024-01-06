package dev.adamko.dokkatoo.dependencies

import dev.adamko.dokkatoo.internal.Attribute
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*


/** Common [Attribute] values for Dokkatoo [Configuration]s. */
@DokkatooInternalApi
class BaseAttributes(
  objects: ObjectFactory,
  moduleName: Provider<String>,
  modulePath: Provider<String>,
) {
  val dokkatooUsage: Usage = objects.named("dev.adamko.dokkatoo")
  val moduleName: Provider<DokkatooAttribute.ModuleName> = moduleName.map { objects.named(it) }
  val modulePath: Provider<DokkatooAttribute.ModulePath> = modulePath.map { objects.named(it) }
  val dokkaPlugins: DokkatooAttribute.Classpath = objects.named("dokka-plugins")
  val dokkaGenerator: DokkatooAttribute.Classpath = objects.named("dokka-generator")
}


/** [Attribute] values for a specific Dokka format. */
@DokkatooInternalApi
class FormatAttributes(
  formatName: String,
  objects: ObjectFactory,
) {
  val format: DokkatooAttribute.Format = objects.named(formatName)
  val moduleDirectory: DokkatooAttribute.ModuleComponent = objects.named("ModuleDirectory")
  val moduleIncludes: DokkatooAttribute.ModuleComponent = objects.named("ModuleIncludes")
}
