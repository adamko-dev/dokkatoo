package dev.adamko.dokkatoo.tests.examples

import dev.adamko.dokkatoo.utils.GradleProjectTest
import dev.adamko.dokkatoo.utils.buildGradleKts
import dev.adamko.dokkatoo.utils.copyExampleProject
import dev.adamko.dokkatoo.utils.file
import dev.adamko.dokkatoo.utils.findFiles
import dev.adamko.dokkatoo.utils.settingsGradleKts
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

class CustomFormatExampleTest : FunSpec({

  val dokkaProject = initDokkaProject(
    GradleProjectTest.projectTestTempDir.resolve("it/examples/custom-format-dokka").toFile()
  )

  val dokkatooProject = initDokkatooProject(
    GradleProjectTest.projectTestTempDir.resolve("it/examples/custom-format-dokkatoo").toFile()
  )

  context("compare dokka and dokkatoo HTML generators") {
    test("expect dokka can generate HTML") {
      val dokkaBuild = dokkaProject.runner
        .withArguments(
          "clean",
          "dokkaCustomFormat",
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
      val dokkaHtmlDir = dokkaProject.projectDir.resolve("build/dokka/customFormat")
      val dokkatooHtmlDir = dokkatooProject.projectDir.resolve("build/dokka/html")

      test("expect file trees are the same") {
        val expectedFileTree = dokkaHtmlDir.toTreeString()
        val actualFileTree = dokkatooHtmlDir.toTreeString()
        println((actualFileTree to expectedFileTree).sideBySide())
        // drop the first line from each, since the directory name is different
        expectedFileTree.substringAfter("\n") shouldBe actualFileTree.substringAfter("\n")
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
    copyExampleProject("custom-format-example/dokka")

    val dokkaVersion = "1.7.20"
    buildGradleKts = buildGradleKts
      .replace(
        Regex("""id\("org\.jetbrains\.dokka"\) version \("[\d.]+"\)"""),
        Regex.escapeReplacement("""id("org.jetbrains.dokka") version "$dokkaVersion""""),
      )
      .replace(
        "org.jetbrains.dokka:dokka-base:1.7.10",
        "org.jetbrains.dokka:dokka-base:1.7.20",
      )

    settingsGradleKts = settingsGradleKts
      .replace(
        """rootProject.name = "dokka-customFormat-example"""",
        """rootProject.name = "customFormat-example"""",
      )
  }
}

private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("custom-format-example/dokkatoo")

    buildGradleKts += """
      |
      |tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask>().configureEach {
      |  dokkaSourceSets.configureEach {
      |    sourceSetScope.set(":dokkaCustomFormat") // only necessary for testing
      |  }
      |}
      |
    """.trimMargin()
  }
}
