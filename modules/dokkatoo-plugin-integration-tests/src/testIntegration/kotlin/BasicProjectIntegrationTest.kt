package dev.adamko.dokkatoo.tests.integration

import dev.adamko.dokkatoo.utils.GradleProjectTest
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.projectTestTempDir
import dev.adamko.dokkatoo.utils.buildGradleKts
import dev.adamko.dokkatoo.utils.copyIntegrationTestProject
import dev.adamko.dokkatoo.utils.findFiles
import dev.adamko.dokkatoo.utils.projectFile
import dev.adamko.dokkatoo.utils.settingsGradleKts
import dev.adamko.dokkatoo.utils.shouldContainAll
import dev.adamko.dokkatoo.utils.shouldNotContainAnyOf
import dev.adamko.dokkatoo.utils.sideBySide
import dev.adamko.dokkatoo.utils.toTreeString
import dev.adamko.dokkatoo.utils.withEnvironment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

/**
 * Integration test for the `it-basic` project in Dokka
 *
 * Runs Dokka & Dokkatoo, and compares the resulting HTML site.
 */
class BasicProjectIntegrationTest : FunSpec({

  val tempDir = projectTestTempDir.resolve("it/it-basic").toFile()

  val dokkatooProject = initDokkatooProject(tempDir.resolve("dokkatoo"))
  val dokkaProject = initDokkaProject(tempDir.resolve("dokka"))

  context("when generating HTML") {
    val dokkaBuild = dokkaProject.runner
      .withArguments(
        "clean",
        "dokkaHtml",
        "--stacktrace",
        "--info",
      ).forwardOutput()
      .withEnvironment(
        "DOKKA_VERSION" to "1.7.20",
      )
      .build()

    val dokkatooBuild = dokkatooProject.runner
      .withArguments(
        "clean",
        "dokkatooGeneratePublicationHtml",
        "--stacktrace",
        "--info",
      ).forwardOutput()
      .build()

    test("expect project builds successfully") {
      dokkatooBuild.output shouldContain "BUILD SUCCESSFUL"
    }

    context("with Dokka") {

      test("expect project builds successfully") {
        dokkaBuild.output shouldContain "BUILD SUCCESSFUL"
      }

      test("expect all dokka workers are successful") {

        val dokkaWorkerLogs = dokkatooProject.findFiles { it.name == "dokka-worker.log" }
//      dokkaWorkerLogs shouldHaveCount 1
        dokkaWorkerLogs.firstOrNull().shouldNotBeNull().should { dokkaWorkerLog ->
          dokkaWorkerLog.shouldBeAFile()
          dokkaWorkerLog.readText().shouldNotContainAnyOf(
            "[ERROR]",
            "[WARN]",
          )
        }
      }
    }

    context("with Dokkatoo") {

      test("expect all dokka workers are successful") {

        val dokkaWorkerLogs = dokkatooProject.findFiles { it.name == "dokka-worker.log" }
//      dokkaWorkerLogs shouldHaveCount 1
        dokkaWorkerLogs.firstOrNull().shouldNotBeNull().should { dokkaWorkerLog ->
          dokkaWorkerLog.shouldBeAFile()
          dokkaWorkerLog.readText().shouldNotContainAnyOf(
            "[ERROR]",
            "[WARN]",
          )
        }
      }
    }

    test("expect the same HTML is generated") {

      val dokkaHtmlDir = dokkaProject.projectDir.resolve("build/dokka/html")
      val dokkatooHtmlDir = dokkatooProject.projectDir.resolve("build/dokka/html")

      val expectedFileTree = dokkaHtmlDir.toTreeString()
      val actualFileTree = dokkatooHtmlDir.toTreeString()
      println((actualFileTree to expectedFileTree).sideBySide())
      expectedFileTree shouldBe actualFileTree

      dokkatooHtmlDir.toFile().shouldHaveSameStructureAs(dokkaHtmlDir.toFile())
      dokkatooHtmlDir.toFile().shouldHaveSameStructureAndContentAs(dokkaHtmlDir.toFile())
    }

    test("Dokkatoo tasks should be cacheable") {
      dokkatooProject.runner.withArguments(
        "dokkatooGeneratePublicationHtml",
        "--stacktrace",
        "--info",
        "--build-cache",
      ).forwardOutput()
        .build().should { buildResult ->
          buildResult.output shouldContainAll listOf(
            "Task :prepareDokkatooParametersHtml UP-TO-DATE",
            "Task :dokkatooGeneratePublicationHtml UP-TO-DATE",
          )
        }
    }

    // TODO test configuration cache
//    test("Dokkatoo tasks should be configuration-cache compatible") {
//      val dokkatooBuildCache =
//        dokkatooProject.runner.withArguments(
//          "clean",
//          "dokkatooGenerate",
//          "--stacktrace",
//          "--info",
//          "--no-build-cache",
//          "--configuration-cache",
//        ).forwardOutput()
//          .build()
//
//      dokkatooBuildCache.output.shouldContainAll(
//        "Task :prepareDokkatooParametersHtml UP-TO-DATE",
//        "Task :dokkatooGeneratePublicationHtml UP-TO-DATE",
//      )
//    }
  }
})


private fun initDokkaProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyIntegrationTestProject("it-basic/dokka")

    buildGradleKts = buildGradleKts
      .replace(
        // no idea why this needs to be changed
        """file("../customResources/""",
        """file("./customResources/""",
      )
      // update relative paths to the template files - they're now in the same directory
      .replace(
        """../template.root.gradle.kts""",
        """./template.root.gradle.kts""",
      )

    // update relative paths to the template files - they're now in the same directory
    settingsGradleKts = settingsGradleKts
      .replace(
        """../template.settings.gradle.kts""",
        """./template.settings.gradle.kts""",
      )

    var templateGradleSettings: String by projectFile("template.settings.gradle.kts")
    templateGradleSettings = templateGradleSettings
      .replace("for-integration-tests-SNAPSHOT", "1.7.20")
  }
}

private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyIntegrationTestProject("it-basic/dokkatoo")
  }
}
