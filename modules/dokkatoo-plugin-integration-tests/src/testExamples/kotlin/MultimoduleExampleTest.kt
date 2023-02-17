package dev.adamko.dokkatoo.tests.examples

import dev.adamko.dokkatoo.utils.*
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.projectTestTempDir
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import kotlin.text.Regex.Companion.escapeReplacement

class MultimoduleExampleTest : FunSpec({

  val dokkaProject = initDokkaProject(
    projectTestTempDir.resolve("it/examples/multimodule-example/dokka").toFile()
  )

  val dokkatooProject = initDokkatooProject(
    projectTestTempDir.resolve("it/examples/multimodule-example/dokkatoo").toFile()
  )

  context("compare dokka and dokkatoo HTML generators") {
    test("expect dokka can generate HTML") {
      val dokkaBuild = dokkaProject.runner
        .withArguments(
          "clean",
          "dokkaHtmlMultiModule",
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
          ":parentProject:dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--info",
        )
        .forwardOutput()
        .build()

      dokkatooBuild.output shouldContain "BUILD SUCCESSFUL"
      dokkatooBuild.output shouldContain "Generation completed successfully"
    }

    context("expect dokka and dokkatoo HTML is the same") {
      val dokkaHtmlDir =
        dokkaProject.projectDir.resolve("parentProject/build/dokka/html")
      val dokkatooHtmlDir = dokkatooProject.projectDir.resolve("parentProject/build/dokka/html")

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
          ":parentProject:dokkatooGeneratePublicationHtml",
          "--info",
          "--stacktrace",
        )
        .forwardOutput()
        .build()

      dokkatooBuild.output shouldContain "BUILD SUCCESSFUL"
      dokkatooBuild.output shouldContain "Generation completed successfully"

      dokkatooProject.runner.withArguments(
        ":parentProject:dokkatooGeneratePublicationHtml",
        "--stacktrace",
        "--info",
        "--build-cache",
      ).forwardOutput()
        .build().should { dokkatooBuildCache ->

          dokkatooBuildCache.output shouldContainAll listOf(
            "> Task :parentProject:prepareDokkatooParametersHtml UP-TO-DATE",
            "> Task :parentProject:dokkatooGeneratePublicationHtml UP-TO-DATE",
            "BUILD SUCCESSFUL",
            "8 actionable tasks: 8 up-to-date",
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
          ":parentProject:dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--info",
          "--no-build-cache",
          "--configuration-cache",
        )
        .forwardOutput()
        .build()

      dokkatooBuild.output shouldContain "BUILD SUCCESSFUL"
    }
  }
})

private fun initDokkaProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("multimodule-example/dokka")

    val dokkaVersion = "1.7.20"
    settingsGradleKts = settingsGradleKts
      .replace(
        Regex("""id\("org\.jetbrains\.dokka"\) version \("[\d.]+"\)"""),
        escapeReplacement("""id("org.jetbrains.dokka") version ("$dokkaVersion")"""),
      ).replace(
        """pluginManagement {""",
        """
          |
          |pluginManagement {
          |    repositories {
          |        gradlePluginPortal()
          |        mavenCentral()
          |        mavenLocal()
          |    }
          |
        """.trimMargin()
      ) + """
        |
        |dependencyResolutionManagement {
        |  repositories {
        |    mavenCentral()
        |    mavenLocal()
        |  }
        |}
        |
      """.trimMargin()

    buildGradleKts = """
    // TODO remove this - I'm just placing it here so I can copy+paste it into parentProject/build.gradle - so I can step-through debug
    // compileOnly("org.jetbrains.dokka:all-modules-page-plugin:1.7.20")
    // compileOnly("org.jetbrains.dokka:dokka-core:1.7.20")
    // compileOnly("org.jetbrains.dokka:dokka-base:1.7.20")
    // compileOnly("org.jetbrains.dokka:dokka-gradle-plugin:1.7.20")
    // compileOnly("org.jetbrains.dokka:templating-plugin:1.7.20")
    // compileOnly("org.jetbrains.dokka:kotlin-analysis-intellij:1.7.20")
    """.trimIndent()

    dir("parentProject") {

      buildGradleKts += """
        |
        |val hackDokkaHtmlDir by tasks.registering(Sync::class) {
        |  // sync directories so the dirs in both dokka and dokkatoo are the same
        |  from(layout.buildDirectory.dir("dokka/htmlMultiModule"))
        |  into(layout.buildDirectory.dir("dokka/html"))
        |}
        |
        |tasks.matching { "dokka" in it.name.toLowerCase() && it.name != hackDokkaHtmlDir.name }.configureEach { 
        |  finalizedBy(hackDokkaHtmlDir)
        |}
        |
      """.trimMargin()
    }
  }
}

private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject("multimodule-example/dokkatoo")
  }
}
