package dev.adamko.dokkatoo.it.examples

import dev.adamko.dokkatoo.utils.*
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.dokkaSrcExampleProjectsDir
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.projectTestTempDir
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import kotlin.text.Regex.Companion.escapeReplacement

class DokkaMultimoduleExampleTest : FunSpec({

  val dokkaProject = initDokkaProject(
    projectTestTempDir.resolve("it/examples/dokka-multimodule-example").toFile()
  )

  val dokkatooProject = initDokkatooProject(
    projectTestTempDir.resolve("it/examples/dokkatoo-multimodule-example").toFile()
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
        dokkaProject.projectDir.resolve("parentProject/build/dokka/htmlMultiModule")
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

      val dokkatooBuildCache =
        dokkatooProject.runner.withArguments(
          ":parentProject:dokkatooGeneratePublicationHtml",
          "--stacktrace",
          "--info",
          "--build-cache",
        ).forwardOutput()
          .build()

      dokkatooBuildCache.output shouldContainAll listOf(
        "Task :prepareDokkatooParametersHtml UP-TO-DATE",
        "Task :dokkatooGeneratePublicationHtml UP-TO-DATE",
      )
      withClue("Dokka Generator should not be triggered, so check it doesn't log anything") {
        dokkatooBuild.output shouldNotContain "Generation completed successfully"
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


private fun GradleProjectTest.copyExampleProject() {
  dokkaSrcExampleProjectsDir
    .resolve("dokka-multimodule-example")
    .toFile()
    .copyRecursively(projectDir.toFile(), overwrite = true) { _, _ -> OnErrorAction.SKIP }
}

private fun initDokkaProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject()

    val dokkaVersion =
      "for-integration-tests-SNAPSHOT"
//      "1.7.20"
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
  }
}

private fun initDokkatooProject(
  destinationDir: File,
): GradleProjectTest {
  return GradleProjectTest(destinationDir.toPath()).apply {
    copyExampleProject()

    settingsGradleKts = """
      |rootProject.name = "dokkatoo-multimodule-example"
      |
      |pluginManagement {
      |    plugins {
      |        kotlin("jvm") version "1.7.20"
      |    }
      |    repositories {
      |        gradlePluginPortal()
      |        mavenCentral()
      |        maven(file("$testMavenRepoRelativePath"))
      |    }
      |}
      |
      |@Suppress("UnstableApiUsage")
      |dependencyResolutionManagement {
      |    repositories {
      |        mavenCentral()
      |        maven(file("$testMavenRepoRelativePath"))
      |    }
      |}
      |
      |include(":parentProject")
      |include(":parentProject:childProjectA")
      |include(":parentProject:childProjectB")
      |
    """.trimMargin()


    file("build.gradle.kts").toFile().delete()

    dir("parentProject") {

      buildGradleKts = """
        |plugins {
        |  kotlin("jvm") version "1.7.20" apply false
        |  id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
        |}
        |
        |dependencies {
        |  dokkatoo(project(":parentProject:childProjectA"))
        |  dokkatoo(project(":parentProject:childProjectB"))
        |  dokkatooPluginHtml("org.jetbrains.dokka:all-modules-page-plugin:1.7.20")
        |  dokkatooPluginHtml("org.jetbrains.dokka:templating-plugin:1.7.20")
        |}
        |
      """.trimMargin()

      dir("childProjectA") {
        buildGradleKts = """
          |plugins {
          |  kotlin("jvm")
          |  id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
          |}
          |
          |dokkatoo {
          |  dokkatooSourceSets.configureEach { 
          |    includes.from("Module.md")
          |  }
          |  //dokkatooSourceSets.configureEach { 
          |  //  includes.from(
          |  //    layout
          |  //      .projectDirectory
          |  //      .dir("src/main")
          |  //      .asFileTree
          |  //      .matching { include("**/Module.md") }
          |  //  )
          |  //}
          |}
          |
        """.trimMargin()
      }
      dir("childProjectB") {
        buildGradleKts = """
          |plugins {
          |  kotlin("jvm")
          |  id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
          |}
          |
          |dokkatoo {
          |  dokkatooSourceSets.configureEach { 
          |    includes.from("Module.md")
          |  }
          |  //dokkatooSourceSets.configureEach { 
          |  //  includes.from(
          |  //    layout
          |  //      .projectDirectory
          |  //      .dir("src/main")
          |  //      .asFileTree
          |  //      .matching { include("**/Module.md") }
          |  //  )
          |  //}
          |}
          |
        """.trimMargin()
      }
    }
  }
}
