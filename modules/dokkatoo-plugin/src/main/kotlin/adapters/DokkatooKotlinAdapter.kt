package dev.adamko.dokkatoo.adapters

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetIDSpec
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.Platform
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation

/**
 * The [DokkatooKotlinAdapter] plugin will automatically register Kotlin source sets as Dokka source sets. *
 *
 * **Must be applied *after* [dev.adamko.dokkatoo.DokkatooBasePlugin]**
 */
@DokkatooInternalApi
abstract class DokkatooKotlinAdapter @Inject constructor(
  private val objects: ObjectFactory,
) : Plugin<Project> {

  private val logger = Logging.getLogger(this::class.java)

  override fun apply(project: Project) {
    logger.info("applied DokkaKotlinAdapter to ${project.path}")

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

    logger.info("Configuring Dokka in Gradle Kotlin Project ${project.path}")

    val dokkatooExtension = project.extensions.getByType<DokkatooExtension>()

    val kotlinPlatformType = kotlinExtension.kotlinPlatformType()

    val allKotlinCompilations: Collection<KotlinCompilation<*>> = when (kotlinExtension) {
      is KotlinMultiplatformExtension   -> kotlinExtension.targets.flatMap { it.compilations }
      is KotlinSingleTargetExtension<*> -> kotlinExtension.target.compilations
      else                              -> emptyList()
    }

    kotlinExtension.sourceSets.all kss@{

      /** Get all compilations for this source set */
      val kssCompilations: List<KotlinCompilation<*>> =
        allKotlinCompilations.filter { compilation ->
          this@kss == compilation.defaultSourceSet
              || this@kss in compilation.kotlinSourceSets
              || this@kss in compilation.allKotlinSourceSets
        }

      val kotlinSourceSetConfigurationNames: List<String> =
        kssCompilations.flatMap { compilationSourceSet ->
          with(compilationSourceSet) {
            sequence {
              yield(implementationConfigurationName)
              yield(runtimeOnlyConfigurationName)
              yield(apiConfigurationName)
              yield(compileOnlyConfigurationName)
              yieldAll(relatedConfigurationNames)
            }
          }
        }

      /** Determine if a source set is 'main', and not test sources */
      val isMainSourceSet: Boolean =
        kssCompilations.isEmpty() || kssCompilations.any { it.isMainCompilation() }

      // TODO: Needs to respect filters.
      //  We probably need to change from "sourceRoots" to support "sourceFiles"
      //  https://github.com/Kotlin/dokka/issues/1215
      val extantKotlinSourceRoots = this@kss.kotlin.sourceDirectories.filter { it.exists() }

      val dependentSourceSetIds = this@kss.dependsOn.map { dependedKss ->
        objects.newInstance<DokkaSourceSetIDSpec>("${project.path}:${this@kss.name}:${dependedKss.name}")
          .apply {
            sourceSetName = dependedKss.name
          }
      }

      logger.info("kotlin source set ${this@kss.name} has source roots: ${extantKotlinSourceRoots.map { it.invariantSeparatorsPath }}")

      dokkatooExtension.dokkatooSourceSets.create(this@kss.name) {
        displayName.convention(
          analysisPlatform.map { platform ->
            name.substringBeforeLast(
              delimiter = "Main",
              missingDelimiterValue = platform.name,
            )
          }
        )
        suppress.convention(!isMainSourceSet)
        sourceRoots.from(extantKotlinSourceRoots)
        classpath.from(getKSSClasspath(project, kotlinSourceSetConfigurationNames))
        analysisPlatform.convention(Platform.fromString(kotlinPlatformType.name))
        dependentSourceSets.addAll(dependentSourceSetIds)
      }
    }
  }

  private fun getKSSClasspath(
    project: Project,
    kotlinSourceSetConfigurationNames: List<String>,
  ): FileCollection {
    val classpathCollector = objects.fileCollection()

    kotlinSourceSetConfigurationNames.mapNotNull { kssConfName ->
      project.configurations.findByName(kssConfName)
    }.filter { conf ->
      conf.isCanBeResolved
    }.forEach { conf ->
      classpathCollector.from(
        conf.incoming
          .artifactView {
            componentFilter {
              it is ModuleComponentIdentifier && it !is ProjectComponentIdentifier
            }
            lenient(true)
          }.files
      )
    }

    return classpathCollector
  }

  @DokkatooInternalApi
  companion object {

    private fun KotlinProjectExtension.kotlinPlatformType(): KotlinPlatformType {
      return when (this) {
        is KotlinMultiplatformExtension   -> {
          targets
            .map { it.platformType }
            .distinct()
            .singleOrNull() ?: KotlinPlatformType.common
        }

        is KotlinSingleTargetExtension<*> -> target.platformType

        else                              -> KotlinPlatformType.common
      }
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
  }
}
