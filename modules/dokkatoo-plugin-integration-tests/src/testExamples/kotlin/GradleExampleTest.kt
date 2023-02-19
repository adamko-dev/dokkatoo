package dev.adamko.dokkatoo.tests.examples

import dev.adamko.dokkatoo.utils.GradleProjectTest
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.projectTestTempDir
import dev.adamko.dokkatoo.utils.buildGradleKts
import dev.adamko.dokkatoo.utils.copyExampleProject
import dev.adamko.dokkatoo.utils.findFiles
import dev.adamko.dokkatoo.utils.shouldContainAll
import dev.adamko.dokkatoo.utils.sideBySide
import dev.adamko.dokkatoo.utils.toTreeString
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
        ).forwardOutput()
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
        ).forwardOutput()
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
          "--info",
          "--stacktrace",
        )
        //.forwardOutput()
        .build()

      dokkatooBuild.output shouldContain "BUILD SUCCESSFUL"

      val dokkaWorkerLogs = dokkatooProject.findFiles { it.name == "dokka-worker.log" }
      dokkaWorkerLogs shouldHaveCount 1
      val dokkaWorkerLog = dokkaWorkerLogs.first()
      dokkaWorkerLog.shouldNotBeNull().shouldBeAFile()
      dokkaWorkerLog.readText() shouldContain "Generation completed successfully"

      dokkatooProject.runner.withArguments(
        ":dokkatooGeneratePublicationHtml",
        "--stacktrace",
        "--info",
        "--build-cache",
      ).forwardOutput()
        .build().should { dokkatooBuildCache ->

          dokkatooBuildCache.output shouldContainAll listOf(
            "> Task :prepareDokkatooParametersHtml UP-TO-DATE",
            "> Task :dokkatooGeneratePublicationHtml UP-TO-DATE",
            "BUILD SUCCESSFUL",
            "2 actionable tasks: 2 up-to-date",
          )
          withClue("Dokka Generator should not be triggered, so check it doesn't log anything") {
            dokkatooBuildCache.output shouldNotContain "Generation completed successfully"
          }
        }
    }

    xtest("expect Dokkatoo is compatible with Gradle Configuration Cache") {
      val dokkatooBuild = dokkatooProject.runner
        .withArguments(
          "clean",
          ":dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--info",
          "--no-build-cache",
          "--configuration-cache",
        ).forwardOutput()
        .build()

      dokkatooBuild.output shouldContain "BUILD SUCCESSFUL"
    }
  }
})


private fun initDokkaProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("gradle-example/dokka")

    val dokkaVersion = "1.7.20"
    buildGradleKts = buildGradleKts
      .replace(
        Regex("""id\("org\.jetbrains\.dokka"\) version \("[\d.]+"\)"""),
        escapeReplacement("""id("org.jetbrains.dokka") version "$dokkaVersion""""),
      )
  }
}

private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("gradle-example/dokkatoo")
  }
}
