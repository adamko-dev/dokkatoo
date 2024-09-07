package dev.adamko.dokkatoo.tests.examples

import dev.adamko.dokkatoo.utils.*
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.exampleProjectDataPath
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.projectTestTempDir
import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import kotlin.io.path.walk

class JavaExampleTest : FunSpec({

  val dokkatooProject = initDokkatooProject(
    projectTestTempDir.resolve("it/examples/java-example/dokkatoo").toFile()
  )

  context("verify dokkatoo HTML") {

    test("expect dokkatoo can generate HTML") {
      dokkatooProject.runner
        .addArguments(
          "clean",
          ":dokkatooGeneratePublicationHtml",
          "--stacktrace",
        )
        .forwardOutput()
        .build {
          output shouldContain "BUILD SUCCESSFUL"
        }
    }

    context("expect dokkatoo HTML matches example-project-data") {
      val exampleDataDir = exampleProjectDataPath.resolve("java-example/dokkatoo/html")
      val dokkatooHtmlDir = dokkatooProject.projectDir.resolve("build/dokka/html")

      test("expect file trees are the same") {
        val expectedFileTree = exampleDataDir.toTreeString()
        val actualFileTree = dokkatooHtmlDir.toTreeString()
        println((actualFileTree to expectedFileTree).sideBySide())
        expectedFileTree shouldBe actualFileTree
      }

      test("expect directories are the same") {
        withClue(
          "dokkatooHtmlDir[${dokkatooHtmlDir.walk().toList()}], " +
              "exampleDataDir[${exampleDataDir.walk().toList()}]"
        ) {
          dokkatooHtmlDir.shouldHaveSameStructureAs(exampleDataDir, skipEmptyDirs = true)
          dokkatooHtmlDir.shouldHaveSameStructureAndContentAs(exampleDataDir, skipEmptyDirs = true)
        }
      }
    }
  }


  context("Gradle caching") {
    test("expect Dokkatoo is compatible with Build Cache") {
      dokkatooProject.runner
        .addArguments(
          "clean",
          ":dokkatooGeneratePublicationHtml",
          "--stacktrace",
        )
        .forwardOutput()
        .build {
          output shouldContain "BUILD SUCCESSFUL"
        }

      dokkatooProject.runner
        .addArguments(
          ":dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--build-cache",
        )
        .forwardOutput()
        .build {
          output shouldContainAll listOf(
            "> Task :my-java-application:dokkatooGenerateModuleHtml UP-TO-DATE",
            "> Task :my-java-features:dokkatooGenerateModuleHtml UP-TO-DATE",
            "> Task :my-java-library:dokkatooGenerateModuleHtml UP-TO-DATE",
            "> Task :dokkatooGeneratePublicationHtml UP-TO-DATE",
            "BUILD SUCCESSFUL",
          )
          output shouldContain when {
            // expect "1 executed" because :checkKotlinGradlePluginConfigurationErrors always runs
            dokkatooProject.versions.gradle < "8.0" -> "20 actionable tasks: 20 up-to-date"
            else                                   -> "18 actionable tasks: 18 up-to-date"
          }
        }
    }

    context("expect Dokkatoo is compatible with Configuration Cache") {
      dokkatooProject
        .findFiles {
          val isCCDir = it.invariantSeparatorsPath.endsWith(".gradle/configuration-cache")
          val isCCReport = it.invariantSeparatorsPath.endsWith("build/reports/configuration-cache")
          it.isDirectory && (isCCReport || isCCDir)
        }.forEach { it.deleteRecursively() }

      val configCacheRunner =
        dokkatooProject.runner
          .addArguments(
            "clean",
            ":dokkatooGeneratePublicationHtml",
            "--stacktrace",
            "--no-build-cache",
            "--configuration-cache",
          )
          .forwardOutput()

      test("first build should store the configuration cache") {
        configCacheRunner.build {
          output shouldContain "BUILD SUCCESSFUL"
          configurationCacheReport().asClue { report ->
            report.cacheAction shouldBe "storing"
            report.totalProblemCount shouldBe 0
          }
        }
      }

      test("second build should reuse the configuration cache") {
        configCacheRunner.build {
          output shouldContain "BUILD SUCCESSFUL"
          output shouldContain "Configuration cache entry reused"
        }
      }
    }
  }
})


private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("java-example/dokkatoo")
  }
}
