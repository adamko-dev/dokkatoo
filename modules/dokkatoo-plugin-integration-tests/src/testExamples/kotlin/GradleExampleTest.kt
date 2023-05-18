package dev.adamko.dokkatoo.tests.examples

import dev.adamko.dokkatoo.internal.DokkatooConstants.DOKKA_VERSION
import dev.adamko.dokkatoo.utils.*
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.projectTestTempDir
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.sequences.shouldHaveCount
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import kotlin.text.Regex.Companion.escapeReplacement

class GradleExampleTest : FunSpec({

  val dokkaProject = initDokkaProject(
    projectTestTempDir.resolve("it/examples/gradle-example/dokka").toFile()
  )

  val dokkatooProject = initDokkatooProject(
    projectTestTempDir.resolve("it/examples/gradle-example/dokkatoo").toFile()
  )

  context("compare dokka and dokkatoo HTML generators") {
    test("expect dokka can generate HTML") {
      val dokkaBuild = dokkaProject.runner
        .withArguments(
          "clean",
          "dokkaHtml",
          "--stacktrace",
          "--info",
        )
        .forwardOutput()
        .build()

      dokkaBuild.output shouldContain "BUILD SUCCESSFUL"
      dokkaBuild.output shouldContain "Generation completed successfully"
    }

    test("expect dokkatoo can generate HTML") {
      val dokkatooBuild = dokkatooProject.runner
        .withArguments(
          "clean",
          ":dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--info",
        )
        .forwardOutput()
        .build()


      dokkatooBuild.output shouldContain "BUILD SUCCESSFUL"

      val dokkaWorkerLogs = dokkatooProject.findFiles { it.name == "dokka-worker.log" }
      dokkaWorkerLogs shouldHaveCount 1
      val dokkaWorkerLog = dokkaWorkerLogs.first()
      dokkaWorkerLog.shouldNotBeNull().shouldBeAFile()
      dokkaWorkerLog.readText() shouldContain "Generation completed successfully"
    }

    context("expect dokka and dokkatoo HTML is the same") {
      val dokkaHtmlDir = dokkaProject.projectDir.resolve("build/dokka/html")
      val dokkatooHtmlDir = dokkatooProject.projectDir.resolve("build/dokka/html")

      test("expect file trees are the same") {
        val expectedFileTree = dokkaHtmlDir.toTreeString()
        val actualFileTree = dokkatooHtmlDir.toTreeString()
        println((actualFileTree to expectedFileTree).sideBySide())
        expectedFileTree shouldBe actualFileTree
      }

      test("expect directories are the same") {
        dokkatooHtmlDir.toFile().shouldHaveSameStructureAs(dokkaHtmlDir.toFile())
        dokkatooHtmlDir.toFile().shouldHaveSameStructureAndContentAs(dokkaHtmlDir.toFile())
      }
    }
  }


  context("Gradle caching") {
    test("expect Dokkatoo is compatible with Gradle Build Cache") {
      val dokkatooBuild = dokkatooProject.runner
        .withArguments(
          "clean",
          ":dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--info",
        )
        .forwardOutput()
        .build()

      dokkatooBuild.output shouldContain "BUILD SUCCESSFUL"

      val dokkaWorkerLogs = dokkatooProject.findFiles { it.name == "dokka-worker.log" }
      dokkaWorkerLogs shouldHaveCount 1
      val dokkaWorkerLog = dokkaWorkerLogs.first()
      dokkaWorkerLog.shouldNotBeNull().shouldBeAFile()
      dokkaWorkerLog.readText() shouldContain "Generation completed successfully"

      dokkatooProject.runner
        .withArguments(
          ":dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--info",
          "--build-cache",
        )
        .forwardOutput()
        .build().should { dokkatooBuildCache ->

          dokkatooBuildCache.output shouldContainAll listOf(
            "> Task :dokkatooGeneratePublicationHtml UP-TO-DATE",
            "BUILD SUCCESSFUL",
            "1 actionable task: 1 up-to-date",
          )
          withClue("Dokka Generator should not be triggered, so check it doesn't log anything") {
            dokkatooBuildCache.output shouldNotContain "Generation completed successfully"
          }
        }
    }

    context("expect Dokkatoo is compatible with Gradle Configuration Cache") {
      dokkatooProject.file(".gradle/configuration-cache").toFile().deleteRecursively()
      dokkatooProject.file("build/reports/configuration-cache").toFile().deleteRecursively()

      val configCacheRunner =
        dokkatooProject.runner
          .withArguments(
            "clean",
            ":dokkatooGeneratePublicationHtml",
            "--stacktrace",
            "--no-build-cache",
            "--configuration-cache",
          )
          .forwardOutput()

      test("first build should store the configuration cache") {
        configCacheRunner.build().should { buildResult ->
          buildResult.output shouldContain "BUILD SUCCESSFUL"
          buildResult.output shouldContain "0 problems were found storing the configuration cache"
        }
      }

      test("second build should reuse the configuration cache") {
        configCacheRunner.build().should { buildResult ->
          buildResult.output shouldContain "BUILD SUCCESSFUL"
          buildResult.output shouldContain "Configuration cache entry reused"
        }
      }
    }
  }
})


private fun initDokkaProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("gradle-example/dokka")

    buildGradleKts = buildGradleKts
      .replace(
        Regex("""id\("org\.jetbrains\.dokka"\) version \("[\d.]+"\)"""),
        escapeReplacement("""id("org.jetbrains.dokka") version "$DOKKA_VERSION""""),
      )
  }
}

private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("gradle-example/dokkatoo")

    buildGradleKts += """
      |
      |tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
      |  generator.dokkaSourceSets.configureEach {
      |    sourceSetScope.set(":dokkaHtml") // only necessary for testing
      |  }
      |}
      |
    """.trimMargin()
  }
}
