package dev.adamko.dokkatoo.adapters

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryExtension
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
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper

@DokkatooInternalApi
abstract class DokkatooAndroidAdapter @Inject constructor(
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
) : Plugin<Project> {

  override fun apply(project: Project) {
    logger.info("applied DokkatooAndroidAdapter to ${project.path}")

    project.plugins.withType<DokkatooBasePlugin>().configureEach {
      project.pluginManager.apply {
        withPlugin("com.android.base") { configure(project, id) }
        withPlugin("com.android.application") { configure(project, id) }
        withPlugin("com.android.library") { configure(project, id) }
      }
    }
  }

  protected fun configure(project: Project, androidPluginId: String) {
    val dokkatooExtension = project.extensions.getByType<DokkatooExtension>()

    // TODO try to remove this android stuff, hopefully fetching files via Configurations is enough
    val androidExt = project.extensions.getByType<BaseExtension>()
    @Suppress("DEPRECATION")
    val androidPlugin = project.plugins.findPlugin(androidPluginId) as? BasePlugin ?: return

    val variants: DomainObjectSet<BaseVariant> =
      objects.domainObjectSet(BaseVariant::class).apply {
        addAllLater(providers.provider {
          when (androidExt) {
            is LibraryExtension -> androidExt.libraryVariants
            is AppExtension     -> androidExt.applicationVariants
            else                -> emptyList()
          }
        })
      }

//    val variantsCompileClasspath = objects.fileCollection().from(
//      providers.provider { variants.map { it.getCompileClasspath(null) } }
//    )

    dokkatooExtension.dokkatooSourceSets.configureEach {

      analysisPlatform.map { platform ->
        val compilationClasspath = objects.fileCollection()
        if (platform == KotlinPlatform.AndroidJVM) {
          // fetch Android JARs using the same method as the Kotlin/Android plugin
          // https://github.com/JetBrains/kotlin/blob/v1.8.21/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/android/AndroidProjectHandler.kt#L294-L301
          compilationClasspath.from(
            providers.provider { AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExt) }
          )
          compilationClasspath.from(
            providers.provider { variants.map { it.getCompileClasspath(null) } }
          )
          compilationClasspath.from(
            providers.provider { variants.map { it.getCompileClasspathArtifacts(null) } }
          )
        }
      }

      val androidClasspath = analysisPlatform.map { analysisPlatform ->
        collectAndroidClasspath(
          project.configurations,
          analysisPlatform,
        )
      }

      classpath.from(androidClasspath)
//
//
//      classpath.from(AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExt))
    }
  }

  private fun collectAndroidClasspath(
    configurations: ConfigurationContainer,
    analysisPlatform: KotlinPlatform,
  ): FileCollection {
    val compilationClasspath = objects.fileCollection()
    if (analysisPlatform != KotlinPlatform.AndroidJVM) return compilationClasspath

    fun collectConfiguration(named: String) {

//      configurations.collectIncomingFiles(named, compilationClasspath)

      // need to fetch JARs explicitly, because Android Gradle Plugin is weird
      // and doesn't seem to register the attributes properly
////          @Suppress("UnstableApiUsage")
//          configurations.collectIncomingFiles(named, compilationClasspath) {
////            withVariantReselection()
//            attributes { attribute(AndroidArtifacts.ARTIFACT_TYPE, JAR.type) }
//            lenient(true)
//          }

//      @Suppress("UnstableApiUsage")
      configurations.collectIncomingFiles(named, compilationClasspath) {
//        withVariantReselection()
        attributes {
          attribute(AndroidArtifacts.ARTIFACT_TYPE, PROCESSED_JAR.type)
        }
        lenient(true)
      }
////      @Suppress("UnstableApiUsage")
//      configurations.collectIncomingFiles(named, compilationClasspath) {
////        withVariantReselection()
//        attributes {
//          attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.JAR.type)
//        }
//        lenient(true)
//      }
////      @Suppress("UnstableApiUsage")
//      configurations.collectIncomingFiles(named, compilationClasspath) {
////        withVariantReselection()
//        attributes {
//          attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.CLASSES_JAR.type)
//        }
//        lenient(true)
//      }
////      @Suppress("UnstableApiUsage")
//      configurations.collectIncomingFiles(named, compilationClasspath) {
////        withVariantReselection()
//        attributes {
//          attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.CLASSES.type)
//        }
//        lenient(true)
//      }
    }

    // fetch android.jar
    collectConfiguration(named = VariantDependencies.CONFIG_NAME_ANDROID_APIS)


//    listOf(
//      "releaseCompilationApi",
//      "releaseCompilationImplementation",
//      "releaseCompilationCompileOnly",
//      "releaseCompilationRuntimeOnly",
//      "releaseApiElements",
//      "releaseRuntimeElements",
//      "releaseCompileClasspath",
//      "releaseRuntimeClasspath",
//      "releaseApi",
//      "releaseImplementation",
//      "releaseCompileOnly",
//      "releaseRuntimeOnly",
//      "releaseApiDependenciesMetadata",
//      "releaseImplementationDependenciesMetadata",
//      "releaseCompileOnlyDependenciesMetadata",
//      "releaseRuntimeOnlyDependenciesMetadata",
//      "api",
//      "implementation",
//      "compileOnly",
//      "runtimeOnly",
//      "apiDependenciesMetadata",
//      "implementationDependenciesMetadata",
//      "compileOnlyDependenciesMetadata",
//      "runtimeOnlyDependenciesMetadata"
//    ).forEach {
//      collectConfiguration(named = it)
//    }

//    collectConfiguration(named = VariantDependencies.CONFIG_NAME_COMPILE)
//    collectConfiguration(named = VariantDependencies.CONFIG_NAME_PUBLISH)
//    collectConfiguration(named = VariantDependencies.CONFIG_NAME_ANNOTATION_PROCESSOR)
//    collectConfiguration(named = VariantDependencies.CONFIG_NAME_API)
//    collectConfiguration(named = VariantDependencies.CONFIG_NAME_APPLICATION)
//    collectConfiguration(named = VariantDependencies.CONFIG_NAME_IMPLEMENTATION)
//    collectConfiguration(named = VariantDependencies.CONFIG_NAME_CORE_LIBRARY_DESUGARING)
//    collectConfiguration(named = "release")
    collectConfiguration(named = "releaseCompileClasspath")
    collectConfiguration(named = "releaseRuntimeClasspath")
//    collectConfiguration(named = "compile")

// releaseCompileClasspath
// releaseRuntimeClasspath
// releaseUnitTestCompileClasspath
// releaseUnitTestRuntimeClasspath

    return compilationClasspath
  }

  @DokkatooInternalApi
  companion object {
    private val logger = Logging.getLogger(DokkatooAndroidAdapter::class.java)
  }
}
