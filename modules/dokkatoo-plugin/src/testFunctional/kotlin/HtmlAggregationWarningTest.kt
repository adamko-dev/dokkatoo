package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.utils.addArguments
import dev.adamko.dokkatoo.utils.build
import dev.adamko.dokkatoo.utils.buildGradleKts
import dev.adamko.dokkatoo.utils.projects.initMultiModuleProject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

class HtmlAggregationWarningTest : FunSpec({
  context("when all-modules-page-plugin is missing") {
    val project = initMultiModuleProject("no-all-pages-plugin")

    project.buildGradleKts += """
      |
      |// hack, to remove all-modules-page-plugin for testing purposes
      |afterEvaluate {
      |  configurations.getByName("dokkatooPluginHtml").dependencies.removeIf {
      |    it.group == "org.jetbrains.dokka" && it.name == "all-modules-page-plugin"
      |  }
      |}
      |
    """.trimMargin()


    project.runner
      .addArguments(
        "clean",
        ":dokkatooGenerate",
        "--stacktrace",
      )
      .forwardOutput()
      .build {
        test("expect warning message is logged") {
          output shouldContain /* language=text */ """
              |[:dokkatooGeneratePublicationHtml] org.jetbrains.dokka:all-modules-page-plugin is missing
              |
              |Publication 'test' in has 2 modules, but plugins classpath does not contain 
              |org.jetbrains.dokka:all-modules-page-plugin, which is required for aggregating HTML modules.
              |
              |all-modules-page-plugin should be added automatically.
              |
              | - verify that the dependency has not been excluded
              | - raise an issue https://github.com/adamko-dev/dokkatoo/issues
            """.trimMargin()
        }
      }
  }
})
