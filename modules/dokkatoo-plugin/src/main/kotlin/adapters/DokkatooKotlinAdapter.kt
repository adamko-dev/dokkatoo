package dev.adamko.dokkatoo.adapters

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetIdSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetIdSpec.Companion.dokkaSourceSetIdSpec
import dev.adamko.dokkatoo.dokka.parameters.KotlinPlatform
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.LocalProjectOnlyFilter
import dev.adamko.dokkatoo.internal.not
import javax.inject.Inject
import org.gradle.api.Named
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
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
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

    val allKotlinCompilationDetails = kotlinExtension.compilationDetails(objects, providers)

    val sourceSetDetails = objects.domainObjectContainer(KotlinSourceSetDetails::class)

    kotlinExtension.sourceSets.all kss@{

      val dependentSourceSets =
        this@kss
          .allDependentSourceSets()
          .fold(objects.fileCollection()) { acc, src ->
            acc.from(src.kotlin.sourceDirectories)
          }

      // TODO: Needs to respect filters.
      //  We probably need to change from "sourceRoots" to support "sourceFiles"
      //  https://github.com/Kotlin/dokka/issues/1215
      val extantSourceDirectories =
        this@kss.kotlin.sourceDirectories.filter { it.exists() }

      // determine the source sets IDs of _other_ source sets that _this_ source depends on.
      val dependentSourceSetIds =
        dokkatooExtension.sourceSetScopeDefault.map { sourceSetScope ->
          this@kss.dependsOn.map { dependedKss ->
            objects.dokkaSourceSetIdSpec(sourceSetScope, dependedKss.name)
          }
        }

      // find all compilation details that this source set needs
      val compilations = allKotlinCompilationDetails.map { allCompilations ->
        allCompilations.filter { compilation ->
          this@kss.name in compilation.allKotlinSourceSetsNames
        }
      }

      sourceSetDetails.register(name) {
        this.dependentSourceSetIds.addAll(dependentSourceSetIds)
        this.sourceSets.from(extantSourceDirectories)
        this.dependentSourceSets.from(dependentSourceSets)
        this.compilations.addAll(compilations)
      }
    }

    // proactively use 'all' so source sets will be available in users' build files if they use `named("...")`
    sourceSetDetails.all details@{

      val kssPlatform = compilations.map { values ->
        values.map { it.kotlinPlatform }
          .distinct()
          .singleOrNull() ?: KotlinPlatform.Common
      }

      val sourceSetConfigurationNames = compilations.map { values ->
        values.flatMap { it.configurationNames }
      }

      val kssClasspath = sourceSetConfigurationNames.map { names ->
        getKSSClasspath(project.configurations, objects, names)
      }

      dokkatooExtension.dokkatooSourceSets.register(this@details.name) {
        // only set source-set specific properties, default values for the other properties
        // (like displayName) are set in DokkatooBasePlugin
        this.suppress.set(!isMainSourceSet())
        this.sourceRoots.from(this@details.sourceSets)
        this.classpath.from(kssClasspath)
        this.analysisPlatform.set(kssPlatform)
        this.dependentSourceSets.addAllLater(this@details.dependentSourceSetIds)
      }
    }
  }

  companion object {

    private val logger = Logging.getLogger(DokkatooKotlinAdapter::class.java)

    private fun KotlinProjectExtension.compilationDetails(
      objects: ObjectFactory,
      providers: ProviderFactory,
    ): ListProperty<KotlinCompilationDetails> {

      val details = objects.listProperty<KotlinCompilationDetails>()

      details.addAll(
        providers.provider {
          val allKotlinCompilations: Collection<KotlinCompilation<*>> = when (this) {
            is KotlinMultiplatformExtension -> targets.flatMap { it.compilations }
            is KotlinSingleTargetExtension<*> -> target.compilations
            else -> emptyList()
          }

          allKotlinCompilations.map { compilation ->
            KotlinCompilationDetails(
              target = compilation.target.name,
              kotlinPlatform = KotlinPlatform.fromString(compilation.platformType.name),
              configurationNames = compilation.allConfigurationNames(),
              allKotlinSourceSetsNames = compilation.allKotlinSourceSets.map { it.name },
              mainCompilation = compilation.isMainCompilation(),
            )
          }
        })

      return details
    }


    /** Recursively get all [KotlinSourceSet]s that this source set depends on */
    private tailrec fun KotlinSourceSet.allDependentSourceSets(
      queue: ArrayDeque<KotlinSourceSet> = ArrayDeque<KotlinSourceSet>().apply { addAll(dependsOn) },
      allDependents: List<KotlinSourceSet> = emptyList(),
    ): List<KotlinSourceSet> {
      val next = queue.removeFirstOrNull() ?: return allDependents
      queue.addAll(next.dependsOn)
      return next.allDependentSourceSets(queue, allDependents + next)
    }

    private fun ExtensionContainer.findKotlinExtension(): KotlinProjectExtension? =
      try {
        findByType()
          ?: findByType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()
      } catch (e: Throwable) {
        when (e) {
          is TypeNotPresentException,
          is ClassNotFoundException,
          is NoClassDefFoundError -> null

          else                    -> throw e
        }
      }

    private fun KotlinCompilation<*>.isMainCompilation(): Boolean {
      return when (this) {
        is KotlinJvmAndroidCompilation ->
          androidVariant is LibraryVariant || androidVariant is ApplicationVariant

        else                           ->
          name == "main"
      }
    }

    /**
     * Get the [Configuration][org.gradle.api.artifacts.Configuration] names of all configurations
     * used to build this [KotlinCompilation] and
     * [its source sets][KotlinCompilation.kotlinSourceSets].
     */
    private fun KotlinCompilation<*>.allConfigurationNames(): Set<String> =
      relatedConfigurationNames union kotlinSourceSets.flatMap { it.relatedConfigurationNames }

    /** Get the classpath used to build and run a Kotlin Source Set */
    private fun getKSSClasspath(
      configurations: ConfigurationContainer,
      objects: ObjectFactory,
      kotlinSourceSetConfigurationNames: List<String>,
    ): FileCollection {
      return kotlinSourceSetConfigurationNames
        .mapNotNull { kssConfName -> configurations.findByName(kssConfName) }
        .filter { conf -> conf.isCanBeResolved }
        .fold(objects.fileCollection()) { classpathCollector, conf ->
          classpathCollector.from(
            @Suppress("UnstableApiUsage")
            conf
              .incoming
              .artifactView {
                componentFilter(!LocalProjectOnlyFilter)
                lenient(true)
              }.artifacts
              .resolvedArtifacts
              .map { artifacts -> artifacts.map { it.file } }
          )
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
private data class KotlinCompilationDetails(
  val target: String,
  val kotlinPlatform: KotlinPlatform,
  val configurationNames: Set<String>,
  val allKotlinSourceSetsNames: List<String>,
  val mainCompilation: Boolean,
)


/**
 * Store the details of all [KotlinSourceSet]s in a configuration cache compatible way.
 */
private abstract class KotlinSourceSetDetails @Inject constructor(
  private val named: String,
) : Named {

  abstract val dependentSourceSetIds: ListProperty<DokkaSourceSetIdSpec>
  abstract val sourceSets: ConfigurableFileCollection
  abstract val dependentSourceSets: ConfigurableFileCollection
  abstract val compilations: ListProperty<KotlinCompilationDetails>

  fun isMainSourceSet(): Provider<Boolean> =
    compilations.map { values ->
      values.isEmpty() || values.any { it.mainCompilation }
    }

  override fun getName(): String = named
}
