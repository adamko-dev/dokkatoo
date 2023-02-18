package dev.adamko.dokkatoo.tests.integration

import dev.adamko.dokkatoo.utils.GradleProjectTest
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.integrationTestProjectsDir
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.projectTestTempDir
import dev.adamko.dokkatoo.utils.buildGradleKts
import dev.adamko.dokkatoo.utils.copyIntegrationTestProject
import dev.adamko.dokkatoo.utils.projectFile
import dev.adamko.dokkatoo.utils.settingsGradleKts
import dev.adamko.dokkatoo.utils.shouldContainAll
import dev.adamko.dokkatoo.utils.sideBySide
import dev.adamko.dokkatoo.utils.toTreeString
import dev.adamko.dokkatoo.utils.withEnvironment
import io.kotest.assertions.withClue
import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import org.junit.jupiter.api.Test

/**
 * Integration test for the `it-basic` project in Dokka
 *
 * Runs Dokka & Dokkatoo, and compares the resulting HTML site.
 */
class BasicProjectIntegrationTest {

  @Test
  fun `test basic project`() {

    val basicProjectSrcDir = integrationTestProjectsDir.resolve("it-basic/dokka")

    val tempDir = projectTestTempDir.resolve("it/it-basic").toFile()

    val dokkaDir = tempDir.resolve("dokka")
    basicProjectSrcDir.toFile()
      .copyRecursively(dokkaDir, overwrite = true) { _, _ -> OnErrorAction.SKIP }

    val dokkaProject = initDokkaProject(dokkaDir)

    val dokkaBuild = dokkaProject.runner
      .withArguments(
        "clean",
        "dokkaHtml",
        "--stacktrace",
        "--info",
      )
      .forwardOutput()
      .withEnvironment(
        "DOKKA_VERSION" to "1.7.20",
      )
      .build()

    dokkaBuild.output shouldContain "BUILD SUCCESSFUL"
    dokkaBuild.output shouldContain "Generation completed successfully"

    val dokkatooDir = tempDir.resolve("dokkatoo")

    val dokkatooProject = initDokkatooProject(dokkatooDir)

    val dokkatooBuild = dokkatooProject.runner
      .withArguments(
        "clean",
        "dokkatooGeneratePublicationHtml",
        "--stacktrace",
        "--info",
      )
      .forwardOutput()
      .build()

    dokkatooBuild.output shouldContain "BUILD SUCCESSFUL"
    dokkatooBuild.output shouldContain "Generation completed successfully"

    val dokkaHtmlDir = dokkaProject.projectDir.resolve("build/dokka/html")
    val dokkatooHtmlDir = dokkatooProject.projectDir.resolve("build/dokka/html")

    val expectedFileTree = dokkaHtmlDir.toTreeString()
    val actualFileTree = dokkatooHtmlDir.toTreeString()
    println((actualFileTree to expectedFileTree).sideBySide())
    expectedFileTree shouldBe actualFileTree

    dokkatooHtmlDir.toFile().shouldHaveSameStructureAs(dokkaHtmlDir.toFile())
    dokkatooHtmlDir.toFile().shouldHaveSameStructureAndContentAs(dokkaHtmlDir.toFile())

    withClue("Dokkatoo tasks should be cacheable") {
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
          withClue("Dokka Generator should not be triggered, so check it doesn't log anything") {
            buildResult.output shouldNotContain "Generation completed successfully"
          }
        }
    }

    // TODO test configuration cache
//    withClue("Dokkatoo tasks should be configuration-cache compatible") {
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
}


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
      .replace(
        """kotlin("jvm")""",
        """kotlin("jvm") version "1.7.22"""",
      )

    // update relative paths to the template files - they're now in the same directory
    settingsGradleKts = settingsGradleKts
      .replace(
        """../template.settings.gradle.kts""",
        """./template.settings.gradle.kts""",
      )

    var templateGradleBuild: String by projectFile("template.root.gradle.kts")
    templateGradleBuild = ""

    var templateGradleSettings: String by projectFile("template.settings.gradle.kts")
    templateGradleSettings = templateGradleSettings
      .replace("for-integration-tests-SNAPSHOT", "1.7.20")
//      .replace(
//        """maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")""",
//        """//maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")""",
//      )
//      .replace(
//        """maven("http""",
//        """//maven("http""",
//      )
    templateGradleSettings = ""
  }
}

private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyIntegrationTestProject("it-basic/dokkatoo")
  }
}
