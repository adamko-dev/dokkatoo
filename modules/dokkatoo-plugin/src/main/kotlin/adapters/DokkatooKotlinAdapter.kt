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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

    val sourceSetDetails = objects.domainObjectContainer(KotlinSourceSetDetails2::class)

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
      val extantKotlinSourceRoots =
        this@kss.kotlin.sourceDirectories.filter { it.exists() }

      val dependentSourceSetIds =
        dokkatooExtension.sourceSetScopeDefault.map { sourceSetScope ->
          this@kss.dependsOn.map { dependedKss ->
            objects.dokkaSourceSetIdSpec(sourceSetScope, dependedKss.name)
          }
        }

      val compilations = allKotlinCompilationDetails.map { allCompilations ->
        allCompilations.filter { compilation ->
          this@kss.name in compilation.combinedSourceSetNames
        }
      }

      sourceSetDetails.register(name) {
        this.dependentSourceSetIds.addAll(dependentSourceSetIds)
        this.sourceSets.from(extantKotlinSourceRoots)
        this.dependentSourceSets.from(dependentSourceSets)
        this.compilations.addAll(compilations)
      }
    }

    sourceSetDetails.all details@{

      val kssPlatform = compilations.map { values ->
        values.map { it.kotlinPlatform }
          .distinct()
          .singleOrNull() ?: KotlinPlatform.Common
      }

      val sourceSetConfigurationNames = compilations.map { values ->
        values.flatMap { it.configurationNames }
      }
      val dependencies = sourceSetConfigurationNames.map { names ->
        getKSSClasspath(project.configurations, objects, names)
      }
      val libraries = compilations.map { values ->
        values.map { it.libraries }.reduce { acc, files -> acc + files }
      }
      val kssClasspath = providers.zip(dependencies, libraries) { deps, libs -> deps + libs }

      dokkatooExtension.dokkatooSourceSets.register(this@details.name) {
        // only set source-set specific properties, default values for the other properties
        // (like displayName) are set in DokkatooBasePlugin
        this.suppress.set(!isMainSourceSet())
        this.sourceRoots.from(this@details.sourceSets)
        this.classpath.from(kssClasspath)
        this.analysisPlatform.set(kssPlatform)
        this.dependentSourceSets.addAllLater(this@details.dependentSourceSetIds)
        this.sourceSetScope.set(dokkatooExtension.sourceSetScopeDefault)
      }
    }

    //region V2 - misses source sets that are created inside the project
//    val sourceSetDetails = kotlinExtension.sourceSets.sourceSetDetails(
//      sourceSetScopeDefault = dokkatooExtension.sourceSetScopeDefault,
//      objects = objects,
//    )
//
//    sourceSetDetails.orNull?.forEach { kss ->
//
//      // Get all compilations for this source set
//      val compilations = allKotlinCompilationDetails.map { allCompilations ->
//        allCompilations.filter { compilation ->
//          kss.name in compilation.combinedSourceSetNames
//        }
//      }
//
//      val isMainSourceSet = compilations.map {
//        it.isEmpty() || it.any { it.mainCompilation }
//      }
//
//      val sourceSetConfigurationNames = compilations.map { it.flatMap { it.configurationNames } }
//      val kssClasspath = sourceSetConfigurationNames.map { names ->
//        getKSSClasspath(project.configurations, objects, names)
//      }
//
//      val kssPlatform = compilations.map {
//        it.map { it.kotlinPlatform }
//          .distinct()
//          .singleOrNull() ?: KotlinPlatform.Common
//      }
//
//      logger.info("creating DokkaSourceSetSpec for kss ${kss.name}")
//
//      dokkatooExtension.dokkatooSourceSets.register(kss.name) {
//        displayName.set(analysisPlatform.map { it.key })
//        suppress.set(!isMainSourceSet)
//        sourceRoots.from(kss.sourceSets)
//        classpath.from(kssClasspath)
//        analysisPlatform.set(kssPlatform)
//        dependentSourceSets.addAll(kss.dependentSourceSetIds)
//      }
//    }
    //endregion

    //region V1 works but source sets are not added proactively
//    val dokkaSourceSets = providers.zip(
//      allKotlinCompilationDetails,
//      sourceSetDetails,
//    ) { compilations, sourceSets ->
//      createDokkaSourceSets(compilations, sourceSets, objects, project.configurations)
//    }
//
//    dokkatooExtension.dokkatooSourceSets.addAllLater(dokkaSourceSets)
    //endregion

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
              configurationNames = compilation.configurationNames(),

              defaultSourceSetName = compilation.defaultSourceSet.name,
              kotlinSourceSetsNames = compilation.kotlinSourceSets.map { it.name },
              allKotlinSourceSetsNames = compilation.allKotlinSourceSets.map { it.name },

              mainCompilation = compilation.isMainCompilation(),

              compileDependencyFiles = compilation.compileDependencyFiles,

//              libraries = compilation.getKotlinCompileTask()?.libraries ?: objects.fileCollection(),
              libraries = objects.fileCollection(),
            )
          }
        })

      return details
    }


//    private fun NamedDomainObjectContainer<KotlinSourceSet>.sourceSetDetails(
//      sourceSetScopeDefault: Property<String>,
//      objects: ObjectFactory,
//    ): ListProperty<KotlinSourceSetDetails> {
//
//      val details = objects.listProperty<KotlinSourceSetDetails>()
//
//      details.addAll(
//        map { kss ->
//          createSourceSetDetails(
//            kss,
//            objects,
//            sourceSetScopeDefault,
//            allKotlinCompilationDetails.map { allCompilations ->
//              allCompilations.filter { compilation ->
//                this@kss.name in compilation.combinedSourceSetNames
//              }
//            },
//          )
//        }
//      )
//
//      return details
//    }


//    private fun createSourceSetDetails(
//      kss: KotlinSourceSet,
//      objects: ObjectFactory,
//      sourceSetScopeDefault: Property<String>,
//      allCompilations: Provider<List<KotlinCompilationDetails>>,
//    ): Provider<KotlinSourceSetDetails> {
//      return sourceSetScopeDefault.map { sourceSetScope ->
//
//        val dependentSourceSets = kss
//          .allDependentSourceSets()
//          .fold(objects.fileCollection()) { acc, src ->
//            acc.from(src.kotlin.sourceDirectories)
//          }
//
//        // TODO: Needs to respect filters.
//        //  We probably need to change from "sourceRoots" to support "sourceFiles"
//        //  https://github.com/Kotlin/dokka/issues/1215
//        val extantKotlinSourceRoots = kss.kotlin.sourceDirectories.filter { it.exists() }
//
//        val dependentSourceSetIds = kss.dependsOn.map { dependedKss ->
//          objects.dokkaSourceSetIdSpec(sourceSetScope, dependedKss.name)
//        }
//
//        val compilations = allCompilations.map { allCompilations ->
//          allCompilations.filter { compilation ->
//            kss.name in compilation.combinedSourceSetNames
//          }
//        }
//
//        KotlinSourceSetDetails(
//          named = kss.name,
//          sourceSets = extantKotlinSourceRoots,
//          dependentSourceSetIds = dependentSourceSetIds,
//          dependentSourceSets = dependentSourceSets,
//          compilations = compilations,
//        )
//      }
//    }

    /** Recursively get all [KotlinSourceSet]s that this source set depends on */
    private tailrec fun KotlinSourceSet.allDependentSourceSets(
      queue: ArrayDeque<KotlinSourceSet> = ArrayDeque<KotlinSourceSet>().apply { addAll(dependsOn) },
      allDependents: List<KotlinSourceSet> = emptyList(),
    ): List<KotlinSourceSet> {
      val next = queue.removeFirstOrNull() ?: return allDependents
      queue.addAll(next.dependsOn)
      return next.allDependentSourceSets(queue, allDependents + next)
    }


//    private fun createDokkaSourceSets(
//      allKotlinCompilationDetails: List<KotlinCompilationDetails>,
//      sourceSets: List<KotlinSourceSetDetails>,
//      objects: ObjectFactory,
//      configurations: ConfigurationContainer,
//    ): List<DokkaSourceSetSpec> {
//      return sourceSets.map {
//        createDokkaSourceSet(it, allKotlinCompilationDetails, objects, configurations)
//      }
//    }

//    private fun createDokkaSourceSet(
//      kss: KotlinSourceSetDetails,
//      allKotlinCompilationDetails: List<KotlinCompilationDetails>,
//      objects: ObjectFactory,
//      configurations: ConfigurationContainer,
//    ): DokkaSourceSetSpec {
//
//      /** Get all compilations for this source set */
//      val kssCompilations =
//        allKotlinCompilationDetails.filter { compilation ->
//          kss.name in compilation.combinedSourceSetNames
//        }
//
////      val compileDependencyFiles = kssCompilations.fold(objects.fileCollection()) { acc, c ->
////        acc.from(c.compileDependencyFiles)
////      }
//
//      val sourceSetConfigurationNames = kssCompilations.flatMap { it.configurationNames }
//
//      val kssPlatform = kssCompilations
//        .map { it.kotlinPlatform }
//        .distinct()
//        .singleOrNull() ?: KotlinPlatform.Common
//
//      // Determine if a source set is 'main', and not test sources
//      val isMainSourceSet = kssCompilations.isEmpty() || kssCompilations.any { it.mainCompilation }
//
//      val kssClasspath = getKSSClasspath(configurations, objects, sourceSetConfigurationNames)// +
////          kss.dependentSourceSets +
////          compileDependencyFiles +
////          kssCompilations.fold(objects.fileCollection()) { acc, c -> acc.from(c.libraries) }
//
//      logger.info(
//        """
//          creating DokkaSourceSetSpec for kss ${kss.name}
//              kssPlatform:$kssPlatform
//              isMainSourceSet:$isMainSourceSet
//              compilations[${kssCompilations.size}]:${kssCompilations.joinToString { "${it.target} (main:${it.mainCompilation})" }}
//              sourceSetConfigurationNames:$sourceSetConfigurationNames
//        """.trimIndent()
//      )
//
//      return objects.newInstance<DokkaSourceSetSpec>(kss.name).apply {
//        displayName.set(
//          analysisPlatform.map { it.key }
////            name.substringBeforeLast(
////              delimiter = "Main",
//////              missingDelimiterValue = "",
////            ).toLowerCase()
//        )
//        suppress.set(!isMainSourceSet)
//        sourceRoots.from(kss.sourceSets)
//        classpath.from(kssClasspath)
//        analysisPlatform.set(kssPlatform)
//        dependentSourceSets.addAll(kss.dependentSourceSetIds)
//      }
//    }

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

    private fun KotlinCompilation<*>.configurationNames(): Set<String> =
      sequence {
        yield(compileDependencyConfigurationName)
        yield(implementationConfigurationName)
        yield(runtimeOnlyConfigurationName)
        yield(apiConfigurationName)
        yield(compileOnlyConfigurationName)
        yieldAll(relatedConfigurationNames)

        yieldAll(kotlinSourceSets.flatMap { it.configurationNames() })
      }.toSet()

    private fun KotlinSourceSet.configurationNames(): Set<String> =
      sequence {
        yield(apiConfigurationName)
        yield(compileOnlyConfigurationName)
        yield(implementationConfigurationName)
        yield(runtimeOnlyConfigurationName)
        yieldAll(relatedConfigurationNames)
      }.toSet()


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


private data class KotlinCompilationDetails(
  val target: String,
  val kotlinPlatform: KotlinPlatform,
  val configurationNames: Set<String>,
  val defaultSourceSetName: String,
  val kotlinSourceSetsNames: List<String>,
  val allKotlinSourceSetsNames: List<String>,
  val mainCompilation: Boolean,
  val compileDependencyFiles: FileCollection,
  val libraries: FileCollection,
) {
  val combinedSourceSetNames =
    listOf(defaultSourceSetName) + kotlinSourceSetsNames + allKotlinSourceSetsNames
}

private data class KotlinSourceSetDetails(
  val named: String,
  val dependentSourceSetIds: List<DokkaSourceSetIdSpec>,
  val sourceSets: FileCollection,
  val dependentSourceSets: FileCollection,
  val compilations: Provider<List<KotlinCompilationDetails>>,
) : Named {
  override fun getName(): String = named
}

private abstract class KotlinSourceSetDetails2 @Inject constructor(
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

private fun KotlinCompilation<*>.getKotlinCompileTask(): KotlinCompile? =
  this.compileKotlinTaskProvider.get() as? KotlinCompile


//private class KotlinSourceSetDetailsFactory(
//
//) : NamedDomainObjectFactory<KotlinSourceSetDetails> {
//  override fun create(name: String): KotlinSourceSetDetails {
//
//    // Get all compilations for this source set
//    val compilations = allKotlinCompilationDetails.map { allCompilations ->
//      allCompilations.filter { compilation ->
//        kss.name in compilation.combinedSourceSetNames
//      }
//    }
//
//    val isMainSourceSet = compilations.map {
//      it.isEmpty() || it.any { it.mainCompilation }
//    }
//
//    val sourceSetConfigurationNames = compilations.map { it.flatMap { it.configurationNames } }
//    val kssClasspath = sourceSetConfigurationNames.map { names ->
//      DokkatooKotlinAdapter.getKSSClasspath(project.configurations, objects, names)
//    }
//
//    val kssPlatform = compilations.map {
//      it.map { it.kotlinPlatform }
//        .distinct()
//        .singleOrNull() ?: KotlinPlatform.Common
//    }
//
//    DokkatooKotlinAdapter.logger.info("creating DokkaSourceSetSpec for kss ${kss.name}")
//
//
//    dokkatooExtension.dokkatooSourceSets.register(kss.name) {
//      displayName.set(analysisPlatform.map { it.key })
//      suppress.set(!isMainSourceSet)
//      sourceRoots.from(kss.sourceSets)
//      classpath.from(kssClasspath)
//      analysisPlatform.set(kssPlatform)
//      dependentSourceSets.addAll(kss.dependentSourceSetIds)
//    }
//  }
//
//}
