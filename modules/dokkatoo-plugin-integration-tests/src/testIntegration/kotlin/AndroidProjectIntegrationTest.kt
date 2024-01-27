package dev.adamko.dokkatoo.tests.integration

import dev.adamko.dokkatoo.utils.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import kotlin.io.path.*

/**
 * Integration test for the `it-android-0` project in Dokka
 *
 * Runs Dokka & Dokkatoo, and compares the resulting HTML site.
 */
class AndroidProjectIntegrationTest : FunSpec({

  val tempDir = GradleProjectTest.projectTestTempDir.resolve("it/it-android-0").toFile()

  val dokkatooProject = initDokkatooProject(tempDir.resolve("dokkatoo"))
  val dokkaProject = initDokkaProject(tempDir.resolve("dokka"))

  context("when generating HTML") {
    context("with Dokka") {
      dokkaProject.runner
        .addArguments(
          "clean",
          "dokkaHtml",
          "--stacktrace",
        )
        .forwardOutput()
        .build {
          test("expect project builds successfully") {
            output shouldContain "BUILD SUCCESSFUL"
          }
        }
    }

    context("with Dokkatoo") {
      dokkatooProject.runner
        .addArguments(
          "clean",
          "dokkatooGeneratePublicationHtml",
          "--stacktrace",
        )
        .forwardOutput()
        .build {
          test("expect project builds successfully") {
            output shouldContain "BUILD SUCCESSFUL"
          }

          test("expect all dokka workers are successful") {
            dokkatooProject
              .findFiles { it.name == "dokka-worker.log" }
              .shouldBeSingleton { dokkaWorkerLog ->
                dokkaWorkerLog.shouldBeAFile()
                dokkaWorkerLog.readText().shouldNotContainAnyOf(
                  "[ERROR]",
                  "[WARN]",
                )
              }
          }
        }
    }

    test("expect the same HTML is generated") {

      val dokkaHtmlDir = dokkaProject.projectDir.resolve("build/dokka/html")

      val dokkatooHtmlDir = dokkatooProject.projectDir.resolve("build/dokka/html")

      val expectedFileTree = dokkaHtmlDir.toTreeString()
      val actualFileTree = dokkatooHtmlDir.toTreeString()
      println((actualFileTree to expectedFileTree).sideBySide())


      // Dokka doesn't seem to generate all output https://github.com/Kotlin/dokka/issues/3474,
      // so only compare Dokkatoo with Dokka if Dokka contains the expected function.
      // Of course, it's possible that Dokka is correct and the function shouldn't be generated!
      val dokkaWorks = dokkaHtmlDir.walk()
        .filter { it.isRegularFile() && it.extension == "html" }
        .any { file ->
          file.useLines { lines ->
            lines.any { line -> "Will show a small happy text" in line }
          }
        }

      if (dokkaWorks) {
        expectedFileTree shouldBe actualFileTree

        dokkatooHtmlDir.toFile().shouldHaveSameStructureAs(dokkaHtmlDir.toFile())
        dokkatooHtmlDir.toFile().shouldHaveSameStructureAndContentAs(dokkaHtmlDir.toFile())
      } else {
        // remove this else branch if Dokka starts generating onCreate() function

        actualFileTree shouldBe /* language=text */ """
          html/
          ├── index.html
          ├── images/
          │   ├── copy-icon.svg
          │   ├── footer-go-to-link.svg
          │   ├── logo-icon.svg
          │   ├── nav-icons/
          │   │   ├── function.svg
          │   │   ├── interface.svg
          │   │   ├── enum.svg
          │   │   ├── typealias-kotlin.svg
          │   │   ├── field-value.svg
          │   │   ├── abstract-class.svg
          │   │   ├── class-kotlin.svg
          │   │   ├── class.svg
          │   │   ├── exception-class.svg
          │   │   ├── annotation-kotlin.svg
          │   │   ├── field-variable.svg
          │   │   ├── abstract-class-kotlin.svg
          │   │   ├── enum-kotlin.svg
          │   │   ├── object.svg
          │   │   ├── interface-kotlin.svg
          │   │   └── annotation.svg
          │   ├── burger.svg
          │   ├── go-to-top-icon.svg
          │   ├── arrow_down.svg
          │   ├── copy-successful-icon.svg
          │   ├── theme-toggle.svg
          │   └── anchor-copy-button.svg
          ├── styles/
          │   ├── font-jb-sans-auto.css
          │   ├── main.css
          │   ├── prism.css
          │   ├── style.css
          │   └── logo-styles.css
          ├── it-android-0/
          │   ├── it.android/
          │   │   ├── index.html
          │   │   ├── -integration-test-activity/
          │   │   │   ├── index.html
          │   │   │   ├── -integration-test-activity.html
          │   │   │   └── on-create.html
          │   │   └── -android-specific-class/
          │   │       ├── index.html
          │   │       ├── create-view.html
          │   │       ├── -android-specific-class.html
          │   │       └── sparse-int-array.html
          │   └── package-list
          ├── scripts/
          │   ├── prism.js
          │   ├── platform-content-handler.js
          │   ├── symbol-parameters-wrapper_deferred.js
          │   ├── navigation-loader.js
          │   ├── main.js
          │   ├── clipboard.js
          │   ├── sourceset_dependencies.js
          │   └── pages.json
          └── navigation.html
        """.trimIndent()

        val onCreateHtml =
          dokkatooHtmlDir.resolve("it-android-0/it.android/-integration-test-activity/on-create.html")
        onCreateHtml.toFile().shouldBeAFile()
        onCreateHtml.readText() shouldContain "Will show a small happy text"
      }
    }

    test("Dokkatoo tasks should be cacheable") {
      dokkatooProject.runner
        .addArguments(
          "dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--build-cache",
        )
        .forwardOutput()
        .build {
          output shouldContainAll listOf(
            "Task :dokkatooGeneratePublicationHtml UP-TO-DATE",
          )
        }
    }

    context("expect Dokkatoo is compatible with Gradle Configuration Cache") {
      dokkatooProject.file(".gradle/configuration-cache").toFile().deleteRecursively()
      dokkatooProject.file("build/reports/configuration-cache").toFile().deleteRecursively()

      val configCacheRunner =
        dokkatooProject.runner
          .addArguments(
            "clean",
            "dokkatooGeneratePublicationHtml",
            "--stacktrace",
            "--no-build-cache",
            "--configuration-cache",
          )
          .forwardOutput()

      test("first build should store the configuration cache") {
        configCacheRunner.build {
          output shouldContain "BUILD SUCCESSFUL"
          output shouldContain "Configuration cache entry stored"
          output shouldNotContain "problems were found storing the configuration cache"
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

private fun initDokkaProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyIntegrationTestProject("it-android-0/dokka")

    gradleProperties = gradleProperties
      .replace(
        "dokka_it_android_gradle_plugin_version=4.2.2",
        "dokka_it_android_gradle_plugin_version=8.0.2",
      )

    file("src/main/AndroidManifest.xml").deleteIfExists()

    buildGradleKts += """
      
      android {
        namespace = "org.jetbrains.dokka.it.android"
      }
      
      java {
          toolchain {
              languageVersion.set(JavaLanguageVersion.of(17))
          }
      }
    """.trimIndent()
  }
}

private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyIntegrationTestProject("it-android-0/dokkatoo")
  }
}
