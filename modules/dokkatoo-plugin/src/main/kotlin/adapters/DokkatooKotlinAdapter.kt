package dev.adamko.dokkatoo.adapters

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.adapters.KotlinCompilationDetails.Companion.extractKotlinCompilationDetails
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

    // first fetch the relevant properties of all KotlinCompilations
    val allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails> =
      extractKotlinCompilationDetails(
        kotlinProjectExtension = kotlinExtension,
        objects = objects,
        providers = providers,
        configurations = project.configurations,
      )

    // second, fetch the relevant properties of the Kotlin source sets
    val sourceSetDetails: NamedDomainObjectContainer<KotlinSourceSetDetails> =
      KotlinSourceSetDetails.extractKotlinSourceSetDetails(
        kotlinSourceSets = kotlinExtension.sourceSets,
        sourceSetScopeDefault = dokkatooExtension.sourceSetScopeDefault,
        allKotlinCompilationDetails = allKotlinCompilationDetails,
        objects = objects,
        providers = providers,
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
      //classpath.from(sourceDirectoriesOfDependents)
      classpath.from(kssClasspath)
      analysisPlatform.set(kssPlatform)
      dependentSourceSets.addAllLater(details.dependentSourceSetIds)
    }
  }

  private fun determineClasspath(
    details: KotlinSourceSetDetails
  ): Provider<FileCollection> {

    val classpath = objects.fileCollection()

    return details.compilations.map { compilations: List<KotlinCompilationDetails> ->
      if (compilations.isNotEmpty()) {
        compilations.fold(classpath) { acc, details ->
          acc.from(details.compilationClasspath)
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
  //val compileDependencyFiles: FileCollection,
  val dependentSourceSetNames: Set<String>,
  val compilationClasspath: FileCollection,
) {

  @DokkatooInternalApi
  companion object {

    internal fun extractKotlinCompilationDetails(
      kotlinProjectExtension: KotlinProjectExtension,
      objects: ObjectFactory,
      providers: ProviderFactory,
      configurations: ConfigurationContainer,
    ): ListProperty<KotlinCompilationDetails> {

      val details = objects.listProperty<KotlinCompilationDetails>()

      details.addAll(
        providers.provider {
          kotlinProjectExtension
            .allKotlinCompilations()
            .map { compilation ->
              createCompilationDetails(
                compilation = compilation,
                objects = objects,
//                providers = providers,
                configurations = configurations,
              )
            }
        })

      return details
    }

    /** Create a single [KotlinCompilationDetails] for [compilation] */
    private fun createCompilationDetails(
      compilation: KotlinCompilation<*>,
      objects: ObjectFactory,
//      providers: ProviderFactory,
      configurations: ConfigurationContainer,
    ): KotlinCompilationDetails {
      val allKotlinSourceSetsNames =
        compilation.allKotlinSourceSets.map { it.name } + compilation.defaultSourceSet.name

//      val compileDependencyFiles = objects.fileCollection()
//      if (!compilation.target.isAndroidTarget()) {
//        compileDependencyFiles.from(providers.provider { compilation.compileDependencyFiles })
////              .from(providers.provider { compilation.runtimeDependencyFiles })
//      }

      val dependentSourceSetNames =
        compilation.defaultSourceSet.dependsOn.map { it.name }

      val compilationClasspath: FileCollection =
        collectKotlinCompilationClasspath(
          kotlinCompilation = compilation,
          objects = objects,
          configurations = configurations,
        )

      return KotlinCompilationDetails(
        target = compilation.target.name,
        kotlinPlatform = KotlinPlatform.fromString(compilation.platformType.name),
        allKotlinSourceSetsNames = allKotlinSourceSetsNames.toSet(),
        mainCompilation = compilation.isMainCompilation(),
        //compileDependencyFiles = compileDependencyFiles,
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
      kotlinCompilation: KotlinCompilation<*>,
      objects: ObjectFactory,
      configurations: ConfigurationContainer,
    ): FileCollection {

      val compilationClasspath = objects.fileCollection()

//      if (kotlinCompilation.target.isAndroidTarget()) {
//        // Workaround for https://youtrack.jetbrains.com/issue/KT-33893
//
//      }

      /**
       * Aggregate the incoming files from a [Configuration][org.gradle.api.artifacts.Configuration]
       * (with name [named]) into [compilationClasspath].
       *
       * Configurations that cannot be
       * [resolved][org.gradle.api.artifacts.Configuration.isCanBeResolved]
       * will be ignored.
       */
      fun collectConfiguration(named: String, builtBy: TaskProvider<*>? = null) {
        val conf = configurations.findByName(named)
        if (conf != null && conf.isCanBeResolved) {

          val incomingFiles =
            @Suppress("UnstableApiUsage")
            conf
              .incoming
              .artifactView { lenient(true) }
              .artifacts
              .resolvedArtifacts
              .map { artifacts -> artifacts.map { it.file } }

          compilationClasspath.from(incomingFiles)

          if (builtBy != null) {
            compilationClasspath.builtBy(builtBy)
          }
        }
      }

      val standardConfigurations = mutableListOf<String>().apply {
        addAll(kotlinCompilation.relatedConfigurationNames)
        addAll(kotlinCompilation.kotlinSourceSets.flatMap { it.relatedConfigurationNames })
      }.toSet()

      standardConfigurations.forEach(::collectConfiguration)

      if (!kotlinCompilation.target.isAndroidTarget()) {
        if (kotlinCompilation is AbstractKotlinNativeCompilation) {
          // K/N doesn't correctly set task dependencies, the configuration
          // `defaultSourceSet.implementationMetadataConfigurationName`
          // will trigger a bunch of Gradle warnings about "using file outputs without task dependencies",
          // so K/N compilations need to explicitly depend on the compilation tasks
          // UPDATE: actually I think is wrong, it's a bug with the K/N 'commonize for IDE' tasks
          // see: https://github.com/Kotlin/dokka/issues/2977
          collectConfiguration(
            named = kotlinCompilation.defaultSourceSet.implementationMetadataConfigurationName,
//          builtBy = kotlinCompilation.compileKotlinTaskProvider
          )
        }
      }

      return compilationClasspath
    }

    private fun KotlinCompilation<*>.isMainCompilation(): Boolean {
      return when (this) {
        is KotlinJvmAndroidCompilation ->
          androidVariant is LibraryVariant || androidVariant is ApplicationVariant

        else                           ->
          name == KotlinCompilation.Companion.MAIN_COMPILATION_NAME//  "main"
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

  @DokkatooInternalApi
  companion object {

    internal fun extractKotlinSourceSetDetails(
      kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
      sourceSetScopeDefault: Provider<String>,
      allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
      objects: ObjectFactory,
      providers: ProviderFactory,
    ): NamedDomainObjectContainer<KotlinSourceSetDetails> {

      val sourceSetDetails = objects.domainObjectContainer(KotlinSourceSetDetails::class)

      kotlinSourceSets.configureEach kss@{
        sourceSetDetails.register(
          kotlinSourceSet = this,
          allKotlinCompilationDetails = allKotlinCompilationDetails,
          sourceSetScopeDefault = sourceSetScopeDefault,
          providers = providers,
          objects = objects,
        )
      }

      return sourceSetDetails
    }

    private fun NamedDomainObjectContainer<KotlinSourceSetDetails>.register(
      kotlinSourceSet: KotlinSourceSet,
      allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
      sourceSetScopeDefault: Provider<String>,
      providers: ProviderFactory,
      objects: ObjectFactory,
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
}
