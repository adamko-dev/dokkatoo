package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.dependencies.BaseDependencyManager
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import dev.adamko.dokkatoo.internal.*
import dev.adamko.dokkatoo.services.DokkatooBuildService
import dev.adamko.dokkatoo.workers.ClassLoaderIsolation
import dev.adamko.dokkatoo.workers.ProcessIsolation
import dev.adamko.dokkatoo.workers.WorkerIsolation
import java.io.Serializable
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.*
import org.gradle.workers.WorkerExecutor

/**
 * Configure the behaviour of the [DokkatooBasePlugin].
 */
abstract class DokkatooExtension
@DokkatooInternalApi
constructor(
  private val objects: ObjectFactory,
  internal val baseDependencyManager: BaseDependencyManager,
  internal val buildService: DokkatooBuildService,
) : ExtensionAware, Serializable {

  /** Directory into which [DokkaPublication]s will be produced */
  abstract val dokkatooPublicationDirectory: DirectoryProperty

  /**
   * Directory into which Dokka Modules will be produced.
   *
   * Note that Dokka Modules are intermediate products and must be combined into a completed
   * Dokka Publication. They are not intended to be comprehensible in isolation.
   */
  abstract val dokkatooModuleDirectory: DirectoryProperty

  @Deprecated("No longer used")
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
   * Defaults to [the Gradle path of the subproject][org.gradle.api.Project.getPath].
   */
  abstract val sourceSetScopeDefault: Property<String>

  /**
   * The Konan home directory, which contains libraries for Kotlin/Native development.
   *
   * This is only required as a workaround to fetch the compile-time dependencies in Kotlin/Native
   * projects with a version below 2.0.
   */
  // This property should be removed when Dokkatoo only supports KGP 2 or higher.
  @DokkatooInternalApi
  abstract val konanHome: RegularFileProperty

  /**
   * Configuration for creating Dokka Publications.
   *
   * Each publication will generate one Dokka site based on the included Dokka Source Sets.
   *
   * The type of site is determined by the Dokka Plugins. By default, an HTML site will be generated.
   */
  val dokkatooPublications: NamedDomainObjectContainer<DokkaPublication> =
    extensions.adding(
      "dokkatooPublications",
      objects.domainObjectContainer { named -> objects.newInstance(named, pluginsConfiguration) }
    )

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
    extensions.adding("dokkatooSourceSets", objects.domainObjectContainer())

  /**
   * Dokka Plugin are used to configure the way Dokka generates a format.
   * Some plugins can be configured via parameters, and those parameters are stored in this
   * container.
   */
  val pluginsConfiguration: DokkaPluginParametersContainer =
    extensions.adding("pluginsConfiguration", objects.dokkaPluginParametersContainer())

  /**
   * Versions of dependencies that Dokkatoo will use to run Dokka Generator.
   *
   * These versions can be set to change the versions of dependencies that Dokkatoo uses defaults,
   * or can be read to align versions.
   */
  val versions: Versions = extensions.adding("versions", objects.newInstance())

  interface Versions : ExtensionAware {

    /** Default version used for Dokka dependencies */
    val jetbrainsDokka: Property<String>
    val jetbrainsMarkdown: Property<String>
    val freemarker: Property<String>
    val kotlinxHtml: Property<String>
    val kotlinxCoroutines: Property<String>

    companion object
  }

  /**
   * Dokkatoo runs Dokka Generator in a separate
   * [Gradle Worker](https://docs.gradle.org/8.5/userguide/worker_api.html).
   *
   * You can control whether Dokkatoo launches Dokka Generator in
   * * a new process, using [ProcessIsolation],
   * * or the current process with an isolated classpath, using [ClassLoaderIsolation].
   *
   * _Aside: Launching [without isolation][WorkerExecutor.noIsolation] is not an option, because
   * running Dokka Generator **requires** an isolated classpath._
   *
   * ```kotlin
   * dokkatoo {
   *   // use the current Gradle process, but with an isolated classpath
   *   workerIsolation = ClassLoaderIsolation()
   *
   *   // launch a new process, optionally controlling the standard JVM options
   *   workerIsolation = ProcessIsolation {
   *     minHeapSize = "2g" // increase minimum heap size
   *     systemProperties.add("someCustomProperty", 123)
   *   }
   * }
   * ```
   *
   * @see WorkerIsolation
   * @see dev.adamko.dokkatoo.workers.ProcessIsolation
   * @see dev.adamko.dokkatoo.workers.ClassLoaderIsolation
   *
   */
  @get:Nested
  abstract val dokkaGeneratorIsolation: Property<WorkerIsolation>

  /**
   * Create a new [ClassLoaderIsolation] options instance.
   *
   * The resulting options must be set into [dokkaGeneratorIsolation].
   */
  fun ClassLoaderIsolation(configure: ClassLoaderIsolation.() -> Unit = {}): ClassLoaderIsolation =
    objects.newInstance<ClassLoaderIsolation>().apply(configure)

  /**
   * Create a new [ProcessIsolation] options.
   *
   * The resulting options instance must be set into [dokkaGeneratorIsolation].
   */
  fun ProcessIsolation(configure: ProcessIsolation.() -> Unit = {}): ProcessIsolation =
    objects.newInstance<ProcessIsolation>().apply(configure)
}
