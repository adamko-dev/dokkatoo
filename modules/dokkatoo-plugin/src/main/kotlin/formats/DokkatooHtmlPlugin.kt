package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters
import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters.Companion.DOKKA_HTML_PARAMETERS_NAME
import dev.adamko.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters
import dev.adamko.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters.Companion.DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.uppercaseFirstChar
import dev.adamko.dokkatoo.tasks.LogHtmlPublicationLinkTask
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

abstract class DokkatooHtmlPlugin
@DokkatooInternalApi
constructor() : DokkatooFormatPlugin(formatName = "html") {

  override fun DokkatooFormatPluginContext.configure() {
    registerDokkaBasePluginConfiguration()
    registerDokkaVersioningPlugin()

    val logHtmlUrlTask = registerLogHtmlUrlTask()

    dokkatooTasks.generatePublication.configure {
      finalizedBy(logHtmlUrlTask)
    }

    //region automatically depend on all-modules-page-plugin if aggregating multiple projects
    val dokkatooIsAggregatingSubprojects = depsManager.incoming.map { dokkatoo ->
      dokkatoo.incoming.artifacts.artifacts.any { artifact ->
        logger.info("${dokkatoo.name} depends on ${artifact.id}, project:${artifact.id.componentIdentifier is ProjectComponentIdentifier}")
        artifact.id.componentIdentifier is ProjectComponentIdentifier
      }
    }

    depsManager.dokkaPluginsClasspath.configure {
      dependencies.addAllLater(dokkatooIsAggregatingSubprojects.map { aggregating ->
        buildList {
          if (aggregating) {
            logger.info("Automatically adding dependency on all-modules-page-plugin")
            add("org.jetbrains.dokka:all-modules-page-plugin")
          }
        }.map { project.dependencies.create(it) }
      })
    }
    //endregion
  }

  private fun DokkatooFormatPluginContext.registerDokkaBasePluginConfiguration() {
    with(dokkatooExtension.pluginsConfiguration) {
      registerBinding(DokkaHtmlPluginParameters::class, DokkaHtmlPluginParameters::class)
      register<DokkaHtmlPluginParameters>(DOKKA_HTML_PARAMETERS_NAME)
      withType<DokkaHtmlPluginParameters>().configureEach {
        separateInheritedMembers.convention(false)
        mergeImplicitExpectActualDeclarations.convention(false)
      }
    }
  }

  private fun DokkatooFormatPluginContext.registerDokkaVersioningPlugin() {
    // register and configure Dokka Versioning Plugin
    with(dokkatooExtension.pluginsConfiguration) {
      registerBinding(
        DokkaVersioningPluginParameters::class,
        DokkaVersioningPluginParameters::class,
      )
      register<DokkaVersioningPluginParameters>(DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME)
      withType<DokkaVersioningPluginParameters>().configureEach {
        renderVersionsNavigationOnAllPages.convention(true)
      }
    }
  }

  private fun DokkatooFormatPluginContext.registerLogHtmlUrlTask():
      TaskProvider<LogHtmlPublicationLinkTask> {

    val generatePublicationTask = dokkatooTasks.generatePublication

    val indexHtmlFile = generatePublicationTask
      .flatMap { it.outputDirectory.file("index.html") }

    val indexHtmlPath = indexHtmlFile.map { indexHtml ->
      indexHtml.asFile
        .relativeTo(project.rootDir.parentFile)
        .invariantSeparatorsPath
    }

    return project.tasks.register<LogHtmlPublicationLinkTask>(
      "logLink" + generatePublicationTask.name.uppercaseFirstChar()
    ) {
      // default port of IntelliJ built-in server is defined in the docs
      // https://www.jetbrains.com/help/idea/settings-debugger.html#24aabda8
      serverUri.convention("http://localhost:63342")
      this.indexHtmlPath.convention(indexHtmlPath)
    }
  }

  @DokkatooInternalApi
  companion object {
    private val logger = Logging.getLogger(DokkatooHtmlPlugin::class.java)
  }
}
