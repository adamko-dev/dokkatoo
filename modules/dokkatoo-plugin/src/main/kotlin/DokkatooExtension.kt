package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.distibutions.DependenciesManager
import dev.adamko.dokkatoo.distibutions.DokkatooConfigurationAttributes
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import dev.adamko.dokkatoo.internal.*
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*

/**
 * Configure the behaviour of the [DokkatooBasePlugin].
 */
abstract class DokkatooExtension
@DokkatooInternalApi
@Inject
constructor(
  project: Project,
  objects: ObjectFactory,
) : ExtensionAware, Serializable {

  /** Directory into which [DokkaPublication]s will be produced */
  abstract val dokkatooPublicationDirectory: DirectoryProperty

  /** Directory into which Dokka Modules will be produced */
  abstract val dokkatooModuleDirectory: DirectoryProperty

  abstract val dokkatooConfigurationsDirectory: DirectoryProperty

  /** Default Dokkatoo cache directory */
  abstract val dokkatooCacheDirectory: DirectoryProperty

  abstract val moduleName: Property<String>
  abstract val moduleVersion: Property<String>
  abstract val modulePath: Property<String>

  /**
   * An arbitrary string used to group source sets that originate from different Gradle subprojects.
   *
   * This is primarily used by Kotlin Multiplatform projects, which can have multiple source sets
   * per subproject.
   *
   * Defaults to [the path of the subproject][org.gradle.api.Project.getPath].
   */
  abstract val sourceSetScopeDefault: Property<String>

  /**
   * Configuration for creating Dokka Publications.
   *
   * Each publication will generate one Dokka site based on the included Dokka Source Sets.
   *
   * The type of site is determined by the Dokka Plugins. By default, an HTML site will be generated.
   */
  val dokkatooPublications: NamedDomainObjectContainer<DokkaPublication> =
    extensions.adding("dokkatooPublications") {
      objects.domainObjectContainer { named ->
        objects.newInstance(named, pluginsConfiguration)
      }
    }

  /**
   * Dokka Source Sets describe the source code that should be included in a Dokka Publication.
   *
   * Dokka will not generate documentation unless there is at least there is at least one Dokka Source Set.
   *
   *  TODO make sure dokkatooSourceSets doc is up to date...
   *
   * Only source sets that are contained within _this project_ should be included here.
   * To merge source sets from other projects, use the Gradle dependencies block.
   *
   * ```kotlin
   * dependencies {
   *   // merge :other-project into this project's Dokka Configuration
   *   dokka(project(":other-project"))
   * }
   * ```
   *
   * Or, to include other Dokka Publications as a Dokka Module use
   *
   * ```kotlin
   * dependencies {
   *   // include :other-project as a module in this project's Dokka Configuration
   *   dokkaModule(project(":other-project"))
   * }
   * ```
   *
   * Dokka will merge Dokka Source Sets from other subprojects if...
   */
  val dokkatooSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec> =
    extensions.adding("dokkatooSourceSets") {
      objects.domainObjectContainer()
    }

  /**
   * Dokka Plugin are used to configure the way Dokka generates a format.
   * Some plugins can be configured via parameters, and those parameters are stored in this
   * container.
   */
  val pluginsConfiguration: DokkaPluginParametersContainer =
    extensions.adding("pluginsConfiguration") {
      objects.dokkaPluginParametersContainer()
    }

  /**
   * Versions of dependencies that Dokkatoo will use to run Dokka Generator.
   *
   * These versions can be set to change the versions of dependencies that Dokkatoo uses defaults,
   * or can be read to align versions.
   */
  val versions: Versions = extensions.adding("versions") {
    objects.newInstance()
  }

  internal val configurationAttributes: DokkatooConfigurationAttributes = objects.newInstance()

  internal val dependenciesManager: DependenciesManager = DependenciesManager(
    project = project,
    d2Attributes = configurationAttributes,
    objects = objects,
  )

  interface Versions : ExtensionAware {

    /** Default version used for Dokka dependencies */
    val jetbrainsDokka: Property<String>
    val jetbrainsMarkdown: Property<String>
    val freemarker: Property<String>
    val kotlinxHtml: Property<String>
    val kotlinxCoroutines: Property<String>

    companion object {
      /** @see DokkatooExtension.versions */
      @Deprecated("explicit `version` property was added")
      @Suppress("unused")
      const val VERSIONS_EXTENSION_NAME = "versions"
    }
  }
}
