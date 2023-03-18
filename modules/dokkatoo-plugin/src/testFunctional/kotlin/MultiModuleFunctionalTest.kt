package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs
import dev.adamko.dokkatoo.internal.DokkatooConstants.DOKKATOO_VERSION
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
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.gradle.testkit.runner.TaskOutcome.*

class MultiModuleFunctionalTest : FunSpec({

  context("when dokkatoo generates all formats") {
    val project = initDokkatooProject("all-formats")

    project.runner
      .withArguments(
        "clean",
        ":dokkatooGenerate",
        "--stacktrace",
      )
      .forwardOutput()
      .build {
        test("expect build is successful") {
          output shouldContain "BUILD SUCCESSFUL"
        }
      }

    test("expect all dokka workers are successful") {
      val dokkaWorkerLogs = project.findFiles { it.name == "dokka-worker.log" }
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
          .shouldContain( /* language=text */ """
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
          "all-modules-page-plugin-1.7.20.jar",
          "dokka-analysis-1.8.10.jar",
          "dokka-base-1.8.10.jar",
          "freemarker-2.3.31.jar",
          "kotlin-analysis-compiler-1.8.10.jar",
          "kotlin-analysis-intellij-1.8.10.jar",
          "kotlinx-html-jvm-0.8.0.jar",
          "templating-plugin-1.8.10.jar",
        )
      }
    }
  }

  context("Gradle caching") {

    context("expect Dokkatoo is compatible with Gradle Build Cache") {
      val project = initDokkatooProject("build-cache")

      test("expect clean is successful") {
        project.runner.withArguments("clean").build {
          output shouldContain "BUILD SUCCESSFUL"
        }
      }

      project.runner
        .withArguments(
          //"clean",
          ":dokkatooGenerate",
          "--stacktrace",
          "--build-cache",
        )
        .forwardOutput()
        .build {
          test("expect build is successful") {
            output shouldContain "BUILD SUCCESSFUL"
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

      context("when build cache is enabled") {
        project.runner
          .withArguments(
            ":dokkatooGenerate",
            "--stacktrace",
            "--build-cache",
          )
          .forwardOutput()
          .build {
            test("expect build is successful") {
              output shouldContainAll listOf(
                "BUILD SUCCESSFUL",
                "36 actionable tasks: 36 up-to-date",
              )
            }

            test("expect all dokkatoo tasks are up-to-date") {
              tasks.filter {
                "dokkatoo" in it.path.substringAfterLast(':').toLowerCase()
              }.shouldForAll {
                it.outcome.shouldBeIn(FROM_CACHE, UP_TO_DATE)
              }
            }
          }
      }
    }

    context("Gradle Configuration Cache") {
      val project = initDokkatooProject("config-cache")

      test("expect clean is successful") {
        project.runner.withArguments("clean").build {
          output shouldContain "BUILD SUCCESSFUL"
        }
      }

      project.runner
        .withArguments(
          //"clean",
          ":dokkatooGenerate",
          "--stacktrace",
          "--no-build-cache",
          "--configuration-cache",
        )
        .forwardOutput()
        .build {
          test("expect build is successful") {
            output shouldContain "BUILD SUCCESSFUL"
          }
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


    context("expect updates in subprojects re-run tasks") {

      val project = initDokkatooProject("submodule-update")

      test("expect clean is successful") {
        project.runner.withArguments("clean").build {
          output shouldContain "BUILD SUCCESSFUL"
        }
      }

      test("expect first build is successful") {
        project.runner
          .withArguments(
            //"clean",
            ":dokkatooGeneratePublicationHtml",
            "--stacktrace",
            "--build-cache",
          )
          .forwardOutput()
          .build {
            output shouldContain "BUILD SUCCESSFUL"
          }
      }

      context("and when a file in a subproject changes") {
        project.dir("subproject-hello") {
          @Suppress("KDocUnresolvedReference")
          createKotlinFile(
            "src/main/kotlin/HelloAgain.kt",
            """
              |package com.project.hello
              |
              |/** Like [Hello], but again */
              |class HelloAgain {
              |    /** prints `Hello Again` to the console */  
              |    fun sayHelloAgain() = println("Hello Again")
              |}
              |
            """.trimMargin()
          )
        }

        context("expect Dokka re-generates the publication") {
          project.runner
            .withArguments(
              ":dokkatooGeneratePublicationHtml",
              "--stacktrace",
              "--build-cache",
            )
            .forwardOutput()
            .build {

              test("expect HelloAgain HTML file exists") {
                val helloAgainIndexHtml = project.projectDir.resolve(
                  "build/dokka/html/subproject-hello/com.project.hello/-hello-again/index.html"
                )

                helloAgainIndexHtml.shouldBeAFile()
              }

              test("expect :subproject-goodbye tasks are up-to-date, because no files changed") {
                shouldHaveTasksWithOutcome(
                  ":subproject-goodbye:prepareDokkatooParametersHtml" to UP_TO_DATE,
                  ":subproject-goodbye:dokkatooGenerateModuleHtml" to UP_TO_DATE,
                  ":subproject-goodbye:prepareDokkatooModuleDescriptorHtml" to UP_TO_DATE,
                )
              }

              val successfulOutcomes = listOf(SUCCESS, FROM_CACHE)
              test("expect :subproject-hello tasks should be re-run, since a file changed") {
                shouldHaveTasksWithAnyOutcome(
                  ":subproject-hello:prepareDokkatooParametersHtml" to successfulOutcomes,
                  ":subproject-hello:dokkatooGenerateModuleHtml" to successfulOutcomes,
                  ":subproject-hello:prepareDokkatooModuleDescriptorHtml" to successfulOutcomes,
                )
              }

              test("expect aggregating tasks should re-run because the :subproject-hello Dokka Module changed") {
                shouldHaveTasksWithAnyOutcome(
                  ":prepareDokkatooParametersHtml" to successfulOutcomes,
                  ":dokkatooGeneratePublicationHtml" to successfulOutcomes,
                )
              }

              test("expect build is be successful") {
                output shouldContain "BUILD SUCCESSFUL"
              }

              test("expect 8 tasks are run") {
                output shouldContain "8 actionable tasks"
              }
            }

          context("and when the class is deleted") {
            project.dir("subproject-hello") {
              createKotlinFile("src/main/kotlin/HelloAgain.kt", "")
            }

            project.runner
              .withArguments(
                ":dokkatooGeneratePublicationHtml",
                "--stacktrace",
                "--info",
                "--build-cache",
              )
              .forwardOutput()
              .build {
                test("expect the generated HTML file is deleted") {
                  val helloAgainIndexHtml = project.projectDir.resolve(
                    "build/dokka/html/subproject-hello/com.project.hello/-hello-again/index.html"
                  )

                  helloAgainIndexHtml.shouldNotExist()
                }
              }
          }
        }
      }
    }
  }

  context("logging -> ") {
    val project = initDokkatooProject("logging")

    test("expect no logs when built using --quiet log level") {

      project.runner
        .withArguments(
          "clean",
          ":dokkatooGenerate",
          "--no-configuration-cache",
          "--no-build-cache",
          "--quiet",
        )
        .forwardOutput()
        .build {
          output.shouldBeEmpty()
        }
    }

    test("expect no Dokkatoo logs when built using lifecycle log level") {

      project.runner
        .withArguments(
          "clean",
          ":dokkatooGenerate",
          "--no-configuration-cache",
          "--no-build-cache",
          "--no-parallel",
          // no logging option => lifecycle log level
        )
        .forwardOutput()
        .build {

          // projects are only configured the first time TestKit runs, and annoyingly there's no
          // easy way to force Gradle to re-configure the projects - so only check conditionally.
          if ("Configure project" in output) {
            output shouldContain /*language=text*/ """
              ¦> Configure project :
              ¦> Configure project :subproject-goodbye
              ¦> Configure project :subproject-hello
              ¦> Task :clean
            """.trimMargin("¦")
          }

          output.lines().sorted().joinToString("\n") shouldContain /*language=text*/ """
            ¦> Task :clean
            ¦> Task :dokkatooGenerate
            ¦> Task :dokkatooGenerateModuleGfm
            ¦> Task :dokkatooGenerateModuleHtml
            ¦> Task :dokkatooGenerateModuleJavadoc
            ¦> Task :dokkatooGenerateModuleJekyll
            ¦> Task :dokkatooGeneratePublicationGfm
            ¦> Task :dokkatooGeneratePublicationHtml
            ¦> Task :dokkatooGeneratePublicationJavadoc
            ¦> Task :dokkatooGeneratePublicationJekyll
            ¦> Task :prepareDokkatooParameters
            ¦> Task :prepareDokkatooParametersGfm
            ¦> Task :prepareDokkatooParametersHtml
            ¦> Task :prepareDokkatooParametersJavadoc
            ¦> Task :prepareDokkatooParametersJekyll
            ¦> Task :subproject-goodbye:clean
            ¦> Task :subproject-goodbye:dokkatooGenerateModuleGfm
            ¦> Task :subproject-goodbye:dokkatooGenerateModuleHtml
            ¦> Task :subproject-goodbye:dokkatooGenerateModuleJavadoc
            ¦> Task :subproject-goodbye:dokkatooGenerateModuleJekyll
            ¦> Task :subproject-goodbye:prepareDokkatooModuleDescriptorGfm
            ¦> Task :subproject-goodbye:prepareDokkatooModuleDescriptorHtml
            ¦> Task :subproject-goodbye:prepareDokkatooModuleDescriptorJavadoc
            ¦> Task :subproject-goodbye:prepareDokkatooModuleDescriptorJekyll
            ¦> Task :subproject-goodbye:prepareDokkatooParametersGfm
            ¦> Task :subproject-goodbye:prepareDokkatooParametersHtml
            ¦> Task :subproject-goodbye:prepareDokkatooParametersJavadoc
            ¦> Task :subproject-goodbye:prepareDokkatooParametersJekyll
            ¦> Task :subproject-hello:clean
            ¦> Task :subproject-hello:dokkatooGenerateModuleGfm
            ¦> Task :subproject-hello:dokkatooGenerateModuleHtml
            ¦> Task :subproject-hello:dokkatooGenerateModuleJavadoc
            ¦> Task :subproject-hello:dokkatooGenerateModuleJekyll
            ¦> Task :subproject-hello:prepareDokkatooModuleDescriptorGfm
            ¦> Task :subproject-hello:prepareDokkatooModuleDescriptorHtml
            ¦> Task :subproject-hello:prepareDokkatooModuleDescriptorJavadoc
            ¦> Task :subproject-hello:prepareDokkatooModuleDescriptorJekyll
            ¦> Task :subproject-hello:prepareDokkatooParametersGfm
            ¦> Task :subproject-hello:prepareDokkatooParametersHtml
            ¦> Task :subproject-hello:prepareDokkatooParametersJavadoc
            ¦> Task :subproject-hello:prepareDokkatooParametersJekyll
          """.trimMargin("¦")
        }
    }
  }
})

private fun initDokkatooProject(
  testName: String,
  config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
  return gradleKtsProjectTest("multi-module-hello-goodbye/$testName") {

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
      |  id("dev.adamko.dokkatoo") version "$DOKKATOO_VERSION"
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
          |    id("dev.adamko.dokkatoo") version "$DOKKATOO_VERSION"
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

      createKotlinFile("src/main/kotlin/HelloAgain.kt", "")
    }

    dir("subproject-goodbye") {

      buildGradleKts = """
          |plugins {
          |    kotlin("jvm") version "1.7.20"
          |    id("dev.adamko.dokkatoo") version "$DOKKATOO_VERSION"
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

    config()
  }
}
