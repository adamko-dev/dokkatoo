package dev.adamko.dokkatoo.dependencies

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.DOKKATOO_CONFIGURATION_NAME
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.declarable
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*


/**
 * Root [Configuration] for fetching all types of Dokkatoo files from other subprojects.
 */
@DokkatooInternalApi
class BaseDependencyManager(
  project: Project,
  moduleNameString: Provider<String>,
  modulePathString: Provider<String>,
  objects: ObjectFactory,
) {
  internal val dokkatooUsage: Usage = objects.named("dev.adamko.dokkatoo")
  internal val moduleName: Provider<DokkatooAttribute.ModuleName> =
    moduleNameString.map { objects.named<DokkatooAttribute.ModuleName>(it) }
  internal val modulePath: Provider<DokkatooAttribute.ModulePath> =
    modulePathString.map { objects.named<DokkatooAttribute.ModulePath>(it) }

  val declaredDependencies: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(DOKKATOO_CONFIGURATION_NAME) {
      description = "Fetch all Dokkatoo files from all configurations in other subprojects"
      declarable()
    }
}
