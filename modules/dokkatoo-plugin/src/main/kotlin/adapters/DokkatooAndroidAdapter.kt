package dev.adamko.dokkatoo.adapters

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.dependency.VariantDependencies
import com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType.CLASSES_JAR
import com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType.PROCESSED_JAR
import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.parameters.KotlinPlatform
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.collectIncomingFiles
import javax.inject.Inject
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*

@DokkatooInternalApi
abstract class DokkatooAndroidAdapter @Inject constructor(
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
) : Plugin<Project> {

  override fun apply(project: Project) {
    logger.info("applied DokkatooAndroidAdapter to ${project.path}")

    project.plugins.withType<DokkatooBasePlugin>().configureEach {
      project.pluginManager.apply {
        withPlugin("com.android.base") { configure(project) }
        withPlugin("com.android.application") { configure(project) }
        withPlugin("com.android.library") { configure(project) }
      }
    }
  }

  protected fun configure(project: Project) {
    val dokkatooExtension = project.extensions.getByType<DokkatooExtension>()

    val androidExt = project.extensions.getByType<BaseExtension>()

    dokkatooExtension.dokkatooSourceSets.configureEach {

      classpath.from(
        analysisPlatform.map { analysisPlatform ->
          when (analysisPlatform) {
            KotlinPlatform.AndroidJVM ->
              AndroidClasspathCollector(
                androidExt = androidExt,
                configurations = project.configurations,
                objects = objects,
                providers = providers,
              )

            else                      ->
              objects.fileCollection()
          }

        }
      )
    }
  }

  @DokkatooInternalApi
  companion object {
    private val logger = Logging.getLogger(DokkatooAndroidAdapter::class.java)
  }
}


/**
 * A utility for determining the classpath of an Android compilation.
 *
 * It's important that this class is separate from [DokkatooAndroidAdapter]. It must be separate
 * because it uses Android Gradle Plugin classes (like [BaseExtension]). Were it not separate, and
 * these classes were present in the function signatures of [DokkatooAndroidAdapter], then when
 * Gradle tries to create a decorated instance of [DokkatooAndroidAdapter] it will if the project
 * does not have the Android Gradle Plugin applied, because the classes will be missing.
 */
private object AndroidClasspathCollector {

  operator fun invoke(
    androidExt: BaseExtension,
    configurations: ConfigurationContainer,
    objects: ObjectFactory,
    providers: ProviderFactory,
  ): FileCollection {
    val compilationClasspath = objects.fileCollection()

    fun collectConfiguration(named: String) {
      @Suppress("UnstableApiUsage")
      listOf(
        // need to fetch multiple different types of files, because AGP is weird and doesn't seem
        // to have a 'just give me normal JVM classes' option
        ARTIFACT_TYPE_ATTRIBUTE to PROCESSED_JAR.type,
        ARTIFACT_TYPE_ATTRIBUTE to CLASSES_JAR.type,
      ).forEach { (attribute, attributeValue) ->
        configurations.collectIncomingFiles(named, compilationClasspath) {
          attributes {
            attribute(attribute, attributeValue)
          }
          lenient(true)
        }
      }
    }

    // fetch android.jar
    collectConfiguration(named = VariantDependencies.CONFIG_NAME_ANDROID_APIS)

    val variantConfigurations = collectVariantConfigurationNames(
      androidExt,
      objects.domainObjectSet(BaseVariant::class),
      providers,
    )

    for (variantConfig in variantConfigurations.get()) {
      collectConfiguration(named = variantConfig)
    }

    return compilationClasspath
  }

  /** Fetch all configuration names used by all variants. */
// fetching _all_ configuration names is very brute force and should probably be refined to
// only fetch those that match a specific DokkaSourceSetSpec
  private fun collectVariantConfigurationNames(
    androidExt: BaseExtension,
    collector: DomainObjectSet<BaseVariant>,
    providers: ProviderFactory,
  ): Provider<List<String>> {

    val variants: DomainObjectSet<BaseVariant> =
      collector.apply {
        addAllLater(providers.provider {
          when (androidExt) {
            is LibraryExtension -> androidExt.libraryVariants
            is AppExtension     -> androidExt.applicationVariants
            is TestExtension    -> androidExt.applicationVariants
            else                -> emptyList()
          }
        })
      }

    return providers.provider {
      variants.flatMap {
        setOf(
          it.compileConfiguration.name,
          it.runtimeConfiguration.name,
          it.annotationProcessorConfiguration.name,
        )
      }
    }
  }
}
