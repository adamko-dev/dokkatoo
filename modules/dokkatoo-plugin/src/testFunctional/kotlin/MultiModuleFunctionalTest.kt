package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs
import dev.adamko.dokkatoo.utils.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class MultiModuleFunctionalTest : FunSpec({

  val project = initDokkatooProject()


  context("when dokkatoo generates all formats") {
    val build = project.runner
      .withArguments(
        "clean",
        ":dokkatooGenerate",
        "--stacktrace",
        "--info",
      )
      .forwardOutput()
      .build()


    test("expect build is successful") {
      build.output shouldContain "BUILD SUCCESSFUL"
    }

    test("expect all dokka workers are successful") {
      val dokkaWorkerLogs = project.findFiles { it.name == "dokka-worker.log" }
//      dokkaWorkerLogs shouldHaveCount 1
      dokkaWorkerLogs.firstOrNull().shouldNotBeNull().should { dokkaWorkerLog ->
        dokkaWorkerLog.shouldBeAFile()
        dokkaWorkerLog.readText().shouldNotContainAnyOf(
          "[ERROR]",
          "[WARN]",
        )
      }
    }

    context("expect HTML site is generated") {

      test("with expected HTML files") {
        project.projectDir.resolve("subproject/build/dokka/html/index.html").shouldBeAFile()
        project.projectDir.resolve("subproject/build/dokka/html/com/project/hello/Hello.html")
          .shouldBeAFile()
      }

      test("and dokka_parameters.json is generated") {
        project.projectDir.resolve("subproject/build/dokka/html/dokka_parameters.json")
          .shouldBeAFile()
      }

      test("with element-list") {
        project.projectDir.resolve("build/dokka/html/package-list").shouldBeAFile()
        project.projectDir.resolve("build/dokka/html/package-list").toFile().readText()
          .shouldContain( // language=text
            """
              |${'$'}dokka.format:html-v1
              |${'$'}dokka.linkExtension:html
              |
              |module:subproject-hello
              |com.project.hello
              |module:subproject-goodbye
              |com.project.goodbye
            """.trimMargin()
          )
      }
    }


//        project.projectDir.toFile().walk().forEach { println(it) }

//        project.projectDir.resolve("subproject/build/dokka-output/com/project/hello/Hello.html").shouldBeAFile()
//        project.projectDir.resolve("subproject/build/dokka-output/index.html").shouldBeAFile()
//        project.projectDir.resolve("subproject/build/dokka-config/dokka_parameters.json").shouldBeAFile()
//        project.projectDir.resolve("subproject/build/dokka-output/element-list").shouldBeAFile()
//        project.projectDir.resolve("subproject/build/dokka-output/element-list").toFile().readText().shouldContain(
//            """
//            ${'$'}dokka.format:javadoc-v1
//            ${'$'}dokka.linkExtension:html
//
//            com.project.hello
//        """.trimIndent()
//        )

    val dokkaConfigurationFile =
      project.projectDir.resolve("build/dokka-config/html/dokka_parameters.json")
    dokkaConfigurationFile.shouldExist()
    dokkaConfigurationFile.shouldBeAFile()
    @OptIn(ExperimentalSerializationApi::class)
    val dokkaConfiguration = kotlinx.serialization.json.Json.decodeFromStream(
      DokkaParametersKxs.serializer(),
      dokkaConfigurationFile.toFile().inputStream(),
    )

    context("expect pluginsClasspath") {
      val pluginClasspathJars = dokkaConfiguration.pluginsClasspath.map { it.name }

      test("does not contain subproject jars") {
        pluginClasspathJars.shouldNotContainAnyOf(
          "subproject-hello.jar",
          "subproject-goodbye.jar",
        )
      }

      test("contains the default Dokka plugins") {
        pluginClasspathJars.shouldContainExactlyInAnyOrder(
//        "markdown-jvm-0.3.1.jar",
          "kotlin-analysis-intellij-1.7.20.jar",
          "dokka-base-1.7.20.jar",
          "templating-plugin-1.7.20.jar",
          "dokka-analysis-1.7.20.jar",
          "kotlin-analysis-compiler-1.7.20.jar",
          "kotlinx-html-jvm-0.8.0.jar",
          "freemarker-2.3.31.jar",
          "all-modules-page-plugin-1.7.20.jar",
        )
      }
    }
  }

  context("Gradle caching") {
    context("expect Dokkatoo is compatible with Gradle Build Cache") {
      // for some weird reason Gradle can't run clean and dokkatooGenerate together
      project.runner.withArguments("clean")

      val build = project.runner
        .withArguments(
          //"clean",
          ":dokkatooGenerate",
          "--stacktrace",
          "--info",
          "--build-cache",
        )
        .forwardOutput()
        .build()

      test("expect build is successful") {
        build.output shouldContain "BUILD SUCCESSFUL"
      }

      test("expect all dokka workers are successful") {
        val dokkaWorkerLogs = project.findFiles { it.name == "dokka-worker.log" }
        dokkaWorkerLogs.forAll { dokkaWorkerLog ->
          dokkaWorkerLog.shouldBeAFile()
          dokkaWorkerLog.readText().shouldNotContainAnyOf(
            "[ERROR]",
            "[WARN]",
          )
        }
      }

      context("when build cache is enabled") {
        project.runner
          .withArguments(
            ":dokkatooGenerate",
            "--stacktrace",
            "--info",
            "--build-cache",
          )
          .forwardOutput()
          .build().let { dokkatooBuildCache ->

            test("expect build is successful") {
              dokkatooBuildCache.output shouldContainAll listOf(
                "BUILD SUCCESSFUL",
                "36 actionable tasks: 36 up-to-date",
              )
            }

            test("expect all dokkatoo tasks are up-to-date") {
              build.tasks.filter {
                "dokkatoo" in it.path.substringAfterLast(':').toLowerCase()
              }.shouldForAll {
                it.outcome.shouldBeIn(FROM_CACHE, UP_TO_DATE)
              }
            }
          }
      }
    }

    context("Gradle Configuration Cache") {
      // for some weird reason Gradle can't run clean and dokkatooGenerate together
      project.runner.withArguments("clean")

      val build = project.runner
        .withArguments(
          //"clean",
          ":dokkatooGenerate",
          "--stacktrace",
          "--info",
          "--no-build-cache",
          "--configuration-cache",
        )
        .forwardOutput()
        .build()

      test("expect build is successful") {
        build.output shouldContain "BUILD SUCCESSFUL"
      }

      test("expect all dokka workers are successful") {
        val dokkaWorkerLogs = project.findFiles { it.name == "dokka-worker.log" }
        dokkaWorkerLogs.forAll { dokkaWorkerLog ->
          dokkaWorkerLog.shouldBeAFile()
          dokkaWorkerLog.readText().shouldNotContainAnyOf(
            "[ERROR]",
            "[WARN]",
          )
        }
      }
    }
  }
})

private fun initDokkatooProject(): GradleProjectTest {
  return gradleKtsProjectTest("multi-module-hello-goodbye") {

    settingsGradleKts += """
      |
      |include(":subproject-hello")
      |include(":subproject-goodbye")
      |
    """.trimMargin()

    buildGradleKts = """
      |plugins {
      |  // Kotlin plugin shouldn't be necessary here, but without it Dokka errors
      |  // with ClassNotFound KotlinPluginExtension... very weird
      |  kotlin("jvm") version "1.7.20" apply false
      |  id("dev.adamko.dokkatoo") version "0.0.3-SNAPSHOT"
      |}
      |
      |dependencies {
      |  dokkatoo(project(":subproject-hello"))
      |  dokkatoo(project(":subproject-goodbye"))
      |  dokkatooPluginHtml("org.jetbrains.dokka:all-modules-page-plugin:1.7.20")
      |}
      |
    """.trimMargin()

    dir("subproject-hello") {
      buildGradleKts = """
          |plugins {
          |    kotlin("jvm") version "1.7.20"
          |    id("dev.adamko.dokkatoo") version "0.0.3-SNAPSHOT"
          |}
          |
        """.trimMargin()

      createKotlinFile(
        "src/main/kotlin/Hello.kt",
        """
          |package com.project.hello
          |
          |/** The Hello class */
          |class Hello {
          |    /** prints `Hello` to the console */  
          |    fun sayHello() = println("Hello")
          |}
          |
        """.trimMargin()
      )
    }

    dir("subproject-goodbye") {

      buildGradleKts = """
          |plugins {
          |    kotlin("jvm") version "1.7.20"
          |    id("dev.adamko.dokkatoo") version "0.0.3-SNAPSHOT"
          |}
          |
        """.trimMargin()

      createKotlinFile(
        "src/main/kotlin/Goodbye.kt",
        """
          |package com.project.goodbye
          |
          |/** The Goodbye class */
          |class Goodbye {
          |    /** prints a goodbye message to the console */  
          |    fun sayHello() = println("Goodbye!")
          |}
          |
        """.trimMargin()
      )
    }
  }
}
