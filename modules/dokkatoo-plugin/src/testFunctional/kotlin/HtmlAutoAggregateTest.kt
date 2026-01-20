package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.internal.DokkatooConstants
import dev.adamko.dokkatoo.utils.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.shouldBe

class HtmlAutoAggregateTest : FunSpec({
  context("html auto-aggregate") {
    val project = initLargeMultiModuleProject()

    project.runner
      .addArguments(
        "clean",
        "dokkatooGenerate",
        "--stacktrace",
      )
      .build {
        fun getFileTree(base: String): String {
          val htmlDir = project.projectDir.resolve("${base}/build/dokka/html")
          return htmlDir.toTreeString()
        }

        test("expect html aggregated in root") {
          getFileTree(".") shouldBe expectedHtmlDocsFileTree
        }
        test("expect html aggregated in :docs1") {
          getFileTree("docs1") shouldBe expectedHtmlDocsFileTree
        }
        test("expect html aggregated in :nested-x:docs2") {
          getFileTree("nested-x/docs2") shouldBe expectedHtmlDocsFileTree
        }
        test("expect html aggregated in :nested-d:docs3") {
          getFileTree("nested-d/docs3") shouldBe expectedHtmlDocsFileTree
        }
      }
  }
})


private fun TestScope.initLargeMultiModuleProject(
  config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {

  // get the FQN of the class that contains the test, so even though multiple
  // tests uses this project it's unlikely that the project dirs clash
  val baseDirName = testCase.descriptor.ids().first().value
    .substringAfter("dev.adamko.dokkatoo.") // drop the package name
    .replaceNonAlphaNumeric()

  return gradleKtsProjectTest(baseDirName) {

    fun createSubProject(path: String) {
      val projectName = path
        .substringAfterLast(":")
        .split("-")
        .joinToString("") { it.uppercaseFirstChar() }

      settingsGradleKts += "include(\"$path\")\n"

      dir(path.drop(1).replace(":", "/")) {
        buildGradleKts = """
          |plugins {
          |  kotlin("jvm") version embeddedKotlinVersion
          |  id("dev.adamko.dokkatoo") version "${DokkatooConstants.DOKKATOO_VERSION}"
          |}
          |
          """.trimMargin()

        createKotlinFile(
          "src/main/kotlin/$projectName.kt",
          """
            |package com.project.${projectName.lowercase()}
            |
            |/** class from Project `$path` */
            |class $projectName {
            |    /** prints `$projectName` to the console */  
            |    fun say() = println("$projectName")
            |}
            |
            """.trimMargin()
        )
      }
    }

    createSubProject(":horse")
    createSubProject(":cricket")
    createSubProject(":nested-x:bumble-bee")
    createSubProject(":nested-x:dog")
    createSubProject(":nested-x:nested-y:nested-z:koala")
    createSubProject(":nested-x:nested-y:nested-z:nested-w:armadillo")

    fun createDocsAggregator(path: String) {
      settingsGradleKts += "include(\"$path\")\n"

      dir(path.drop(1).replace(":", "/")) {
        buildGradleKts = """
          |plugins {
          |  kotlin("jvm") version embeddedKotlinVersion
          |  id("dev.adamko.dokkatoo") version "${DokkatooConstants.DOKKATOO_VERSION}"
          |}
          |
          |dokkatoo {
          |  pluginsConfiguration {
          |    html {
          |      enableAutomaticAggregation = true
          |    }
          |  }
          |}
          """.trimMargin()
      }
    }

    createDocsAggregator(":")
    createDocsAggregator(":docs1")
    createDocsAggregator(":nested-x:docs2")
    createDocsAggregator(":nested-d:docs3")

    config()
  }
}

private val expectedHtmlDocsFileTree = """
html/
├── cricket/
│   ├── com.project.cricket/
│   │   ├── -cricket/
│   │   │   ├── -cricket.html
│   │   │   ├── index.html
│   │   │   └── say.html
│   │   └── index.html
│   ├── index.html
│   └── navigation.html
├── horse/
│   ├── com.project.horse/
│   │   ├── -horse/
│   │   │   ├── -horse.html
│   │   │   ├── index.html
│   │   │   └── say.html
│   │   └── index.html
│   ├── index.html
│   └── navigation.html
├── images/
│   ├── nav-icons/
│   │   ├── abstract-class-kotlin.svg
│   │   ├── abstract-class.svg
│   │   ├── annotation-kotlin.svg
│   │   ├── annotation.svg
│   │   ├── class-kotlin.svg
│   │   ├── class.svg
│   │   ├── enum-kotlin.svg
│   │   ├── enum.svg
│   │   ├── exception-class.svg
│   │   ├── field-value.svg
│   │   ├── field-variable.svg
│   │   ├── function.svg
│   │   ├── interface-kotlin.svg
│   │   ├── interface.svg
│   │   ├── object.svg
│   │   └── typealias-kotlin.svg
│   ├── anchor-copy-button.svg
│   ├── arrow_down.svg
│   ├── burger.svg
│   ├── copy-icon.svg
│   ├── copy-successful-icon.svg
│   ├── footer-go-to-link.svg
│   ├── go-to-top-icon.svg
│   ├── homepage.svg
│   ├── logo-icon.svg
│   └── theme-toggle.svg
├── nested-x/
│   ├── bumble-bee/
│   │   ├── com.project.bumblebee/
│   │   │   ├── -bumble-bee/
│   │   │   │   ├── -bumble-bee.html
│   │   │   │   ├── index.html
│   │   │   │   └── say.html
│   │   │   └── index.html
│   │   ├── index.html
│   │   └── navigation.html
│   ├── dog/
│   │   ├── com.project.dog/
│   │   │   ├── -dog/
│   │   │   │   ├── -dog.html
│   │   │   │   ├── index.html
│   │   │   │   └── say.html
│   │   │   └── index.html
│   │   ├── index.html
│   │   └── navigation.html
│   └── nested-y/
│       └── nested-z/
│           ├── koala/
│           │   ├── com.project.koala/
│           │   │   ├── -koala/
│           │   │   │   ├── -koala.html
│           │   │   │   ├── index.html
│           │   │   │   └── say.html
│           │   │   └── index.html
│           │   ├── index.html
│           │   └── navigation.html
│           └── nested-w/
│               └── armadillo/
│                   ├── com.project.armadillo/
│                   │   ├── -armadillo/
│                   │   │   ├── -armadillo.html
│                   │   │   ├── index.html
│                   │   │   └── say.html
│                   │   └── index.html
│                   ├── index.html
│                   └── navigation.html
├── scripts/
│   ├── clipboard.js
│   ├── main.js
│   ├── navigation-loader.js
│   ├── pages.json
│   ├── platform-content-handler.js
│   ├── prism.js
│   ├── sourceset_dependencies.js
│   └── symbol-parameters-wrapper_deferred.js
├── styles/
│   ├── font-jb-sans-auto.css
│   ├── logo-styles.css
│   ├── main.css
│   ├── prism.css
│   └── style.css
├── index.html
├── navigation.html
└── package-list
""".trim()
