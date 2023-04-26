package dev.adamko.dokkatoo.adapters

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetIdSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetIdSpec.Companion.dokkaSourceSetIdSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import dev.adamko.dokkatoo.dokka.parameters.KotlinPlatform
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.not
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation

/**
 * The [DokkatooKotlinAdapter] plugin will automatically register Kotlin source sets as Dokka source sets.
 *
 * This is not a standalone plugin, it requires [dev.adamko.dokkatoo.DokkatooBasePlugin] is also applied.
 */
@DokkatooInternalApi
abstract class DokkatooKotlinAdapter @Inject constructor(
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
) : Plugin<Project> {

  override fun apply(project: Project) {
    logger.info("applied DokkatooKotlinAdapter to ${project.path}")

    project.plugins.withType<DokkatooBasePlugin>().configureEach {
      project.pluginManager.apply {
        withPlugin("org.jetbrains.kotlin.android") { exec(project) }
        withPlugin("org.jetbrains.kotlin.js") { exec(project) }
        withPlugin("org.jetbrains.kotlin.jvm") { exec(project) }
        withPlugin("org.jetbrains.kotlin.multiplatform") { exec(project) }
      }
    }
  }

  private fun exec(project: Project) {
    val kotlinExtension = project.extensions.findKotlinExtension() ?: run {
      logger.info("could not find Kotlin Extension")
      return
    }
    logger.info("Configuring Dokkatoo in Gradle Kotlin Project ${project.path}")

    val dokkatooExtension = project.extensions.getByType<DokkatooExtension>()

    val compilationDetailsBuilder = KotlinCompilationDetailsBuilder(
      providers = providers,
      objects = objects,
      configurations = project.configurations,
      projectPath = project.path,
    )

    val sourceSetDetailsBuilder = KotlinSourceSetDetailsBuilder(
      providers = providers,
      objects = objects,
      sourceSetScopeDefault = dokkatooExtension.sourceSetScopeDefault,
      projectPath = project.path,
    )

    // first fetch the relevant properties of all KotlinCompilations
    val allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails> =
      compilationDetailsBuilder.createCompilationDetails(
        kotlinProjectExtension = kotlinExtension,
      )

    // second, fetch the relevant properties of the Kotlin source sets
    val sourceSetDetails: NamedDomainObjectContainer<KotlinSourceSetDetails> =
      sourceSetDetailsBuilder.createSourceSetDetails(
        kotlinSourceSets = kotlinExtension.sourceSets,
        allKotlinCompilationDetails = allKotlinCompilationDetails,
      )

    // for each Kotlin source set, register a Dokkatoo source set
    registerDokkatooSourceSets(
      dokkatooExtension = dokkatooExtension,
      sourceSetDetails = sourceSetDetails,
    )
  }

  /** Register a [DokkaSourceSetSpec] for each element in [sourceSetDetails] */
  private fun registerDokkatooSourceSets(
    dokkatooExtension: DokkatooExtension,
    sourceSetDetails: NamedDomainObjectContainer<KotlinSourceSetDetails>,
  ) {
    // proactively use 'all' so source sets will be available in users' build files if they use `named("...")`
    sourceSetDetails.all details@{
      dokkatooExtension.dokkatooSourceSets.register(details = this@details)
    }
  }

  /** Register a single [DokkaSourceSetSpec] for [details] */
  private fun NamedDomainObjectContainer<DokkaSourceSetSpec>.register(
    details: KotlinSourceSetDetails
  ) {
    val kssPlatform = details.compilations.map { values: List<KotlinCompilationDetails> ->
      values.map { it.kotlinPlatform }
        .distinct()
        .singleOrNull() ?: KotlinPlatform.Common
    }

    val kssClasspath = determineClasspath(details)

    register(details.name) dss@{
      // only set source-set specific properties, default values for the other properties
      // (like displayName) are set in DokkatooBasePlugin
      suppress.set(!details.isMainSourceSet())
      sourceRoots.from(details.sourceDirectories)
      classpath.from(kssClasspath)
      analysisPlatform.set(kssPlatform)
      dependentSourceSets.addAllLater(details.dependentSourceSetIds)
    }
  }

  private fun determineClasspath(
    details: KotlinSourceSetDetails
  ): Provider<FileCollection> {
    return details.compilations.map { compilations: List<KotlinCompilationDetails> ->
      val classpath = objects.fileCollection()

      if (compilations.isNotEmpty()) {
        compilations.fold(classpath) { acc, compilation ->
          acc.from(compilation.compilationClasspath)
//          acc.from(compilation.compileDependencyFiles)
        }
      } else {
        classpath
          .from(details.sourceDirectories)
          .from(details.sourceDirectoriesOfDependents)
      }
    }
  }

  @DokkatooInternalApi
  companion object {

    private val logger = Logging.getLogger(DokkatooKotlinAdapter::class.java)

    private fun ExtensionContainer.findKotlinExtension(): KotlinProjectExtension? =
      try {
        findByType()
        // fallback to trying to get the JVM extension
        // (not sure why I did this... maybe to be compatible with really old versions?)
          ?: findByType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()
      } catch (e: Throwable) {
        when (e) {
          is TypeNotPresentException,
          is ClassNotFoundException,
          is NoClassDefFoundError -> null

          else                    -> throw e
        }
      }
  }
}


/**
 * Store the details of all [KotlinCompilation]s in a configuration cache compatible way.
 *
 * The compilation details may come from a multiplatform project ([KotlinMultiplatformExtension])
 * or a single-platform project ([KotlinSingleTargetExtension]).
 */
@DokkatooInternalApi
private data class KotlinCompilationDetails(
  val target: String,
  val kotlinPlatform: KotlinPlatform,
  val allKotlinSourceSetsNames: Set<String>,
  val mainCompilation: Boolean,
  val compileDependencyFiles: FileCollection,
  val dependentSourceSetNames: Set<String>,
  val compilationClasspath: FileCollection,
)

/** Utility class, encapsulating logic for building [KotlinCompilationDetails] */
private class KotlinCompilationDetailsBuilder(
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
  private val configurations: ConfigurationContainer,
  /** Used for logging */
  private val projectPath: String,
) {
  private val logger = Logging.getLogger(KotlinCompilationDetails::class.java)

  fun createCompilationDetails(
    kotlinProjectExtension: KotlinProjectExtension,
  ): ListProperty<KotlinCompilationDetails> {

    val details = objects.listProperty<KotlinCompilationDetails>()

    details.addAll(
      providers.provider {
        kotlinProjectExtension
          .allKotlinCompilations()
          .map { compilation ->
            createCompilationDetails(compilation = compilation)
          }
      })

    return details
  }

  /** Create a single [KotlinCompilationDetails] for [compilation] */
  private fun createCompilationDetails(
    compilation: KotlinCompilation<*>,
  ): KotlinCompilationDetails {
    val allKotlinSourceSetsNames =
      compilation.allKotlinSourceSets.map { it.name } + compilation.defaultSourceSet.name

    val compileDependencyFiles = objects.fileCollection()
      .from(providers.provider { compilation.compileDependencyFiles })

    val dependentSourceSetNames =
      compilation.defaultSourceSet.dependsOn.map { it.name }

    val compilationClasspath: FileCollection =
      collectKotlinCompilationClasspath(compilation = compilation)

    return KotlinCompilationDetails(
      target = compilation.target.name,
      kotlinPlatform = KotlinPlatform.fromString(compilation.platformType.name),
      allKotlinSourceSetsNames = allKotlinSourceSetsNames.toSet(),
      mainCompilation = compilation.isMainCompilation(),
      compileDependencyFiles = compileDependencyFiles,
      dependentSourceSetNames = dependentSourceSetNames.toSet(),
      compilationClasspath = compilationClasspath,
    )
  }

  private fun KotlinProjectExtension.allKotlinCompilations(): Collection<KotlinCompilation<*>> =
    when (this) {
      is KotlinMultiplatformExtension -> targets.flatMap { it.compilations }
      is KotlinSingleTargetExtension  -> target.compilations
      else                            -> emptyList() // shouldn't happen?
    }

  /**
   * Get the [Configuration][org.gradle.api.artifacts.Configuration] names of all configurations
   * used to build this [KotlinCompilation] and
   * [its source sets][KotlinCompilation.kotlinSourceSets].
   */
  private fun collectKotlinCompilationClasspath(
    compilation: KotlinCompilation<*>,
  ): FileCollection {

    val compilationClasspath = objects.fileCollection()

//      if (kotlinCompilation.target.isAndroidTarget()) {
//        // Workaround for https://youtrack.jetbrains.com/issue/KT-33893
//
//      }

    val standardConfigurations = mutableListOf<String>().apply {
      addAll(compilation.relatedConfigurationNames)
      addAll(compilation.kotlinSourceSets.flatMap { it.relatedConfigurationNames })
    }.toSet()

    logger.info("[$projectPath] compilation ${compilation.name} has ${standardConfigurations.size} standard configurations $standardConfigurations")

    standardConfigurations.forEach { compilationClasspath.collectConfiguration(it) }

    if (!compilation.target.isAndroidTarget()) {
      if (compilation is AbstractKotlinNativeCompilation) {
        // K/N doesn't correctly set task dependencies, the configuration
        // `defaultSourceSet.implementationMetadataConfigurationName`
        // will trigger a bunch of Gradle warnings about "using file outputs without task dependencies",
        // so K/N compilations need to explicitly depend on the compilation tasks
        // UPDATE: actually I think is wrong, it's a bug with the K/N 'commonize for IDE' tasks
        // see: https://github.com/Kotlin/dokka/issues/2977
        compilationClasspath.collectConfiguration(
          named = compilation.defaultSourceSet.implementationMetadataConfigurationName,
//          builtBy = kotlinCompilation.compileKotlinTaskProvider
        )
      }
    }

    return compilationClasspath
  }

  /**
   * Aggregate the incoming files from a [Configuration][org.gradle.api.artifacts.Configuration]
   * (with name [named]) into this [ConfigurableFileCollection].
   *
   * Configurations that cannot be
   * [resolved][org.gradle.api.artifacts.Configuration.isCanBeResolved]
   * will be ignored.
   */
  private fun ConfigurableFileCollection.collectConfiguration(
    named: String,
    builtBy: TaskProvider<*>? = null,
  ) {
    val conf = configurations.findByName(named)
    if (conf != null && conf.isCanBeResolved) {

      val incomingFiles =
        conf
          .incoming
          // ignore failures: it's okay if fetching files is best-effort because maybe
          // Dokka doesn't need _all_ dependencies
          .artifactView { lenient(true) }
          .artifacts
          .artifactFiles

      this@collectConfiguration.from(incomingFiles)

      if (builtBy != null) {
        this@collectConfiguration.builtBy(builtBy)
      }
    }
  }

  companion object {
    private fun KotlinCompilation<*>.isMainCompilation(): Boolean {
      return when (this) {
        is KotlinJvmAndroidCompilation ->
          androidVariant is LibraryVariant || androidVariant is ApplicationVariant

        else                           ->
          name == KotlinCompilation.Companion.MAIN_COMPILATION_NAME
      }
    }

    private fun KotlinTarget.isAndroidTarget(): Boolean =
      platformType == KotlinPlatformType.androidJvm
  }
}


/**
 * Store the details of all [KotlinSourceSet]s in a configuration cache compatible way.
 *
 * @param[named] Should be [KotlinSourceSet.getName]
 */
@DokkatooInternalApi
private abstract class KotlinSourceSetDetails @Inject constructor(
  private val named: String,
) : Named {

  /** Direct source sets that this source set depends on */
  abstract val dependentSourceSetIds: SetProperty<DokkaSourceSetIdSpec>
  abstract val sourceDirectories: ConfigurableFileCollection
  /** _All_ source directories from any (recursively) dependant source set */
  abstract val sourceDirectoriesOfDependents: ConfigurableFileCollection
  /** The specific compilations used to build this source set */
  abstract val compilations: ListProperty<KotlinCompilationDetails>

  /** Estimate if this Kotlin source set are 'main' sources (as opposed to 'test' sources). */
  fun isMainSourceSet(): Provider<Boolean> =
    compilations.map { values ->
      values.isEmpty() || values.any { it.mainCompilation }
    }

  override fun getName(): String = named
}

/** Utility class, encapsulating logic for building [KotlinCompilationDetails] */
private class KotlinSourceSetDetailsBuilder(
  private val sourceSetScopeDefault: Provider<String>,
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
  /** Used for logging */
  private val projectPath: String,
) {

  private val logger = Logging.getLogger(KotlinSourceSetDetails::class.java)

  fun createSourceSetDetails(
    kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
    allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
  ): NamedDomainObjectContainer<KotlinSourceSetDetails> {

    val sourceSetDetails = objects.domainObjectContainer(KotlinSourceSetDetails::class)

    kotlinSourceSets.configureEach kss@{
      sourceSetDetails.register(
        kotlinSourceSet = this,
        allKotlinCompilationDetails = allKotlinCompilationDetails,
      )
    }

    return sourceSetDetails
  }

  private fun NamedDomainObjectContainer<KotlinSourceSetDetails>.register(
    kotlinSourceSet: KotlinSourceSet,
    allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
  ) {

    // TODO: Needs to respect filters.
    //  We probably need to change from "sourceRoots" to support "sourceFiles"
    //  https://github.com/Kotlin/dokka/issues/1215
    val extantSourceDirectories = providers.provider {
      kotlinSourceSet.kotlin.sourceDirectories.filter { it.exists() }
    }

    val compilations = allKotlinCompilationDetails.map { allCompilations ->
      allCompilations.filter { compilation ->
        kotlinSourceSet.name in compilation.allKotlinSourceSetsNames
      }
    }

    // determine the source sets IDs of _other_ source sets that _this_ source depends on.
    val dependentSourceSets = providers.provider { kotlinSourceSet.dependsOn }
    val dependentSourceSetIds =
      providers.zip(
        dependentSourceSets,
        sourceSetScopeDefault,
      ) { sourceSets, sourceSetScope ->
        logger.info("[$projectPath] source set ${kotlinSourceSet.name} has ${sourceSets.size} dependents ${sourceSets.joinToString { it.name }}")
        sourceSets.map { dependedKss ->
          objects.dokkaSourceSetIdSpec(sourceSetScope, dependedKss.name)
        }
      }

    val sourceDirectoriesOfDependents = providers.provider {
      kotlinSourceSet
        .allDependentSourceSets()
        .fold(objects.fileCollection()) { acc, sourceSet ->
          acc.from(sourceSet.kotlin.sourceDirectories)
        }
    }

    register(kotlinSourceSet.name) {
      this.dependentSourceSetIds.addAll(dependentSourceSetIds)
      this.sourceDirectories.from(extantSourceDirectories)
      this.sourceDirectoriesOfDependents.from(sourceDirectoriesOfDependents)
      this.compilations.addAll(compilations)
    }
  }

  /**
   * Return a list containing _all_ source sets that this source set depends on,
   * searching recursively.
   *
   * @see KotlinSourceSet.dependsOn
   */
  private tailrec fun KotlinSourceSet.allDependentSourceSets(
    queue: Set<KotlinSourceSet> = dependsOn.toSet(),
    allDependents: List<KotlinSourceSet> = emptyList(),
  ): List<KotlinSourceSet> {
    val next = queue.firstOrNull() ?: return allDependents
    return next.allDependentSourceSets(
      queue = (queue - next) union next.dependsOn,
      allDependents = allDependents + next,
    )
  }
}
