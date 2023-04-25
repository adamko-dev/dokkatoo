package dev.adamko.dokkatoo.adapters

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.*

@DokkatooInternalApi
abstract class DokkatooAndroidAdapter @Inject constructor() : Plugin<Project> {
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
      enableAndroidDocumentationLink.set(true)
    }
  }

  @DokkatooInternalApi
  companion object {
    private val logger = Logging.getLogger(DokkatooAndroidAdapter::class.java)
  }
}
