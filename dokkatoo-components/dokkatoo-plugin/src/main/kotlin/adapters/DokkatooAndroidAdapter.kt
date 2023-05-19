package dev.adamko.dokkatoo.adapters

import com.android.build.gradle.internal.dependency.VariantDependencies
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.parameters.KotlinPlatform
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.collectIncomingFiles
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*

@DokkatooInternalApi
abstract class DokkatooAndroidAdapter @Inject constructor(
  private val objects: ObjectFactory,
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

    dokkatooExtension.dokkatooSourceSets.configureEach {

      val androidClasspath: Provider<FileCollection> =
        analysisPlatform.map {
          val compilationClasspath = objects.fileCollection()
          if (it == KotlinPlatform.AndroidJVM) {

            fun collectConfiguration(named: String) {
              project.configurations.collectIncomingFiles(named, compilationClasspath)
              // need to fetch JARs explicitly, because Android Gradle Plugin is weird
              // and doesn't seem to register the attributes properly
              @Suppress("UnstableApiUsage")
              project.configurations.collectIncomingFiles(named, compilationClasspath) {
                withVariantReselection()
                attributes {
                  attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.JAR.type)
                }
                lenient(true)
              }
            }

            // fetch android.jar
            collectConfiguration(named = VariantDependencies.CONFIG_NAME_ANDROID_APIS)
          }

          compilationClasspath
        }

      classpath.from(androidClasspath)
    }
  }

  @DokkatooInternalApi
  companion object {
    private val logger = Logging.getLogger(DokkatooAndroidAdapter::class.java)
  }
}
