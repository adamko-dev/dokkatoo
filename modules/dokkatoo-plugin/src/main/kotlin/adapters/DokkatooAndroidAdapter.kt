package dev.adamko.dokkatoo.adapters

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.dependency.VariantDependencies
import com.android.build.gradle.internal.publishing.AndroidArtifacts
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
              collectAndroidClasspath(
                androidExt = androidExt,
                configurations = project.configurations,
              )

            else                      ->
              objects.fileCollection()
          }

        }
      )
    }
  }

  private fun collectAndroidClasspath(
    androidExt: BaseExtension,
    configurations: ConfigurationContainer,
  ): FileCollection {
    val compilationClasspath = objects.fileCollection()

    fun collectConfiguration(named: String) {
      configurations.collectIncomingFiles(named, compilationClasspath) {
        attributes {
          attribute(AndroidArtifacts.ARTIFACT_TYPE, PROCESSED_JAR.type)
        }
        lenient(true)
      }
    }

    // fetch android.jar
    collectConfiguration(named = VariantDependencies.CONFIG_NAME_ANDROID_APIS)

    val variantConfigurations = variantConfigurationNames(androidExt)
    // fetching _all_ configuration names is very brute force and should probably be refined to
    // only fetch those that match a specific DokkaSourceSetSpec

    for (variantConfig in variantConfigurations.get()) {
      collectConfiguration(named = variantConfig)
    }

    return compilationClasspath
  }

  /** Fetch all configuration names used by all variants. */
  private fun variantConfigurationNames(androidExt: BaseExtension): Provider<List<String>> {

    val variants: DomainObjectSet<BaseVariant> =
      objects.domainObjectSet(BaseVariant::class).apply {
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

  @DokkatooInternalApi
  companion object {
    private val logger = Logging.getLogger(DokkatooAndroidAdapter::class.java)
  }
}
