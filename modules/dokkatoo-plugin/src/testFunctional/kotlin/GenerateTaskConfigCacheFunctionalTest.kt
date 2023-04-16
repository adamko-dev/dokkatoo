package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.internal.DokkatooConstants
import dev.adamko.dokkatoo.utils.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

class GenerateTaskConfigCacheFunctionalTest : FunSpec({

  context("given a Kotlin/JVM project") {
    val project = initProject()

    context("when dokkaSourceSets are configured in task :dokkatooGenerateModuleHtml") {
      context("when configuration cache is enabled") {

        val projectRunner = project.runner
          .withArguments(
            "clean",
            ":docs:dokkatooGeneratePublicationHtml",
            "--stacktrace",
            "--configuration-cache",
          )
          .forwardOutput()

        project.file(".gradle/configuration-cache").toFile().deleteRecursively()
        project.file("build/reports/configuration-cache").toFile().deleteRecursively()

        test("first build should store the configuration cache") {
          projectRunner.build {
            output shouldContain "BUILD SUCCESSFUL"
            output shouldContain "0 problems were found storing the configuration cache"
          }
        }

        test("second build should reuse the configuration cache") {
          projectRunner.build {
            output shouldContain "BUILD SUCCESSFUL"
            output shouldContain "Configuration cache entry reused"
          }
        }
      }
    }
  }
})

private fun initProject(): GradleProjectTest {
  return gradleKtsProjectTest("generate-task-test") {

    settingsGradleKts += """
      |
      |include(":subproject-hello")
      |include(":docs")
      |
    """.trimMargin()

    buildGradleKts = """
      |plugins {
      |  // Kotlin plugin shouldn't be necessary here, but without it Dokka errors
      |  // with ClassNotFound KotlinPluginExtension... very weird
      |  kotlin("jvm") version "1.7.20" apply false
      |  id("dev.adamko.dokkatoo") version "${DokkatooConstants.DOKKATOO_VERSION}"
      |}
      |
    """.trimMargin()


    dir("subproject-hello") {
      buildGradleKts = """
        |plugins {
        |  `embedded-kotlin`
        |  id("dev.adamko.dokkatoo") version "${DokkatooConstants.DOKKATOO_VERSION}"
        |}
        |
        |tasks.dokkatooGenerateModuleHtml.configure {
        |  dokkaSourceSets.configureEach {
        |    sourceLink {
        |      localDirectory.set(file("src/main/kotlin"))
        |      val relativeProjectPath = projectDir.relativeToOrNull(rootDir)?.invariantSeparatorsPath ?: ""
        |      remoteUrl("https://github.com/adamko-dev/dokkatoo/tree/main/'${'$'}relativeProjectPath/src/main/kotlin")
        |    }
        |  }
        |}
        |
      """.trimMargin()

      dir("src/main/kotlin") {
        createKotlinFile(
          "Hello.kt",
          """
            |package com.project.hello
            |
            |/** The Hello class */
            |class Hello {
            |  /** prints `Hello` to the console */  
            |  fun sayHello() = println("Hello")
            |}
            |
          """.trimMargin()
        )
      }
    }

    dir("docs") {

      buildGradleKts = """
        |plugins {
        |  `embedded-kotlin`
        |  id("dev.adamko.dokkatoo") version "${DokkatooConstants.DOKKATOO_VERSION}"
        |}
        |
        |dependencies {
        |  dokkatoo(project(":subproject-hello"))
        |}
        |
      """.trimMargin()
    }
  }
}
