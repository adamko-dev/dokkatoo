package dev.adamko.dokkatoo.it

import dev.adamko.dokkatoo.utils.*
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.integrationTestProjectsDir
import dev.adamko.dokkatoo.utils.GradleProjectTest.Companion.projectTestTempDir
import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.junit.jupiter.api.Test

/**
 * Integration test for the `it-basic` project in Dokka
 *
 * Runs Dokka & Dokkatoo, and compares the resulting HTML site.
 */
class BasicProjectIntegrationTest {


  @Test
  fun `test basic project`() {

    val basicProjectSrcDir =
      integrationTestProjectsDir.resolve("it-basic").toFile()
    val templateRootGradleKts =
      integrationTestProjectsDir.resolve("template.root.gradle.kts").toFile()
    val templateSettingsGradleKts =
      integrationTestProjectsDir.resolve("template.settings.gradle.kts").toFile()

    val tempDir = projectTestTempDir.resolve("it-basic").toFile()

    val dokkaDir = tempDir.resolve("dokka")
    basicProjectSrcDir.copyRecursively(dokkaDir, overwrite = true) { _, _ -> OnErrorAction.SKIP }
    templateRootGradleKts.copyInto(directory = dokkaDir, overwrite = true)
    templateSettingsGradleKts.copyInto(directory = dokkaDir, overwrite = true)

    val dokkaProject = GradleProjectTest(dokkaDir.toPath()).apply {
      buildGradleKts = buildGradleKts
        .replace(
          // no idea why this needs to be changed
          """file("../customResources/""",
          """file("./customResources/""",
        )

      // update relative paths to the template files - they're now in the same directory
      settingsGradleKts = settingsGradleKts
        .replace(
          """../template.settings.gradle.kts""",
          """./template.settings.gradle.kts""",
        )
      buildGradleKts = buildGradleKts
        .replace(
          """../template.root.gradle.kts""",
          """./template.root.gradle.kts""",
        )
    }

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
    basicProjectSrcDir.copyRecursively(dokkatooDir, overwrite = true) { _, _ -> OnErrorAction.SKIP }

    val dokkatooProject = GradleProjectTest(dokkatooDir.toPath()).apply {
      buildGradleKts = """
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.kotlinSourceSet

plugins {
  kotlin("jvm") version "1.7.20"
  id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
}

version = "1.7.20-SNAPSHOT"

dependencies {
  implementation(kotlin("stdlib"))
  testImplementation(kotlin("test-junit"))
}

dokkatoo {
  moduleNameDefault.set("Basic Project")
  dokkatooSourceSets.configureEach {
    documentedVisibilities(
      DokkaConfiguration.Visibility.PUBLIC,
      DokkaConfiguration.Visibility.PROTECTED,
    )
    suppressedFiles.from(file("src/main/kotlin/it/suppressedByPath"))
    perPackageOption {
      matchingRegex.set("it.suppressedByPackage.*")
      suppress.set(true)
    }
    perPackageOption {
      matchingRegex.set("it.overriddenVisibility.*")
      documentedVisibilities.set(
        setOf(DokkaConfiguration.Visibility.PRIVATE)
      )
    }
    sourceLink {
      localDirectory.set(file("src/main"))
      remoteUrl.set(
        uri(
          "https://github.com/Kotlin/dokka/tree/master/integration-tests/gradle/projects/it-basic/src/main"
        ).toURL()
      )
    }
  }
  dokkatooPublications.configureEach {
    suppressObviousFunctions.set(true)
    pluginsConfiguration.create("org.jetbrains.dokka.base.DokkaBase") {
      serializationFormat.set(DokkaConfiguration.SerializationFormat.JSON)
      values.set(
        ${"\"\"\""}
          { 
            "customStyleSheets": [
              "${'$'}{file("./customResources/logo-styles.css").invariantSeparatorsPath}", 
              "${'$'}{file("./customResources/custom-style-to-add.css").invariantSeparatorsPath}"
            ], 
            "customAssets": [
              "${'$'}{file("./customResources/custom-resource.svg").invariantSeparatorsPath}"
            ] 
          }
        ${"\"\"\""}.trimIndent()
      )
    }
    suppressObviousFunctions.set(false)
  }
}


""".trimIndent()

      settingsGradleKts = """
rootProject.name = "dokkatoo-it-basic"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(file("$testMavenRepoRelativePath"))
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven(file("$testMavenRepoRelativePath"))
    }
}

""".trimIndent()
    }

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

    val expectedFileTree = dokkaHtmlDir.toPrettyTreeString()
    val actualFileTree = dokkatooHtmlDir.toPrettyTreeString()
    println(actualFileTree)
    expectedFileTree shouldBe actualFileTree

    dokkatooHtmlDir.toFile().shouldHaveSameStructureAs(dokkaHtmlDir.toFile())
    dokkatooHtmlDir.toFile().shouldHaveSameStructureAndContentAs(dokkaHtmlDir.toFile())
  }
}


private val expectedDokkaConf: DokkaConfiguration = parseJson<DokkaConfigurationImpl>(
// language=json
  """
{
  "moduleName": "Basic Project",
  "moduleVersion": "1.7.20-SNAPSHOT",
  "outputDir": ".../build/dokka/html",
  "cacheRoot": null,
  "offlineMode": false,
  "sourceSets": [
    {
      "displayName": "jvm",
      "sourceSetID": {
        "scopeId": ":dokkaHtml",
        "sourceSetName": "main"
      },
      "classpath": [
        ".../kotlin-stdlib-1.7.20.jar",
        ".../kotlin-stdlib-common-1.7.20.jar",
        ".../annotations-13.0.jar"
      ],
      "sourceRoots": [
        ".../src/main/kotlin",
        ".../src/main/java"
      ],
      "dependentSourceSets": [],
      "samples": [],
      "includes": [],
      "includeNonPublic": false,
      "reportUndocumented": false,
      "skipEmptyPackages": true,
      "skipDeprecated": false,
      "jdkVersion": 8,
      "sourceLinks": [
        {
          "localDirectory": ".../src/main",
          "remoteUrl": "https://github.com/Kotlin/dokka/tree/master/integration-tests/gradle/projects/it-basic/src/main",
          "remoteLineSuffix": "#L"
        }
      ],
      "perPackageOptions": [
        {
          "matchingRegex": "it.suppressedByPackage.*",
          "includeNonPublic": false,
          "reportUndocumented": false,
          "skipDeprecated": false,
          "suppress": true,
          "documentedVisibilities": [
            "PUBLIC"
          ]
        },
        {
          "matchingRegex": "it.overriddenVisibility.*",
          "includeNonPublic": false,
          "reportUndocumented": false,
          "skipDeprecated": false,
          "suppress": false,
          "documentedVisibilities": [
            "PRIVATE"
          ]
        }
      ],
      "externalDocumentationLinks": [
        {
          "url": "https://docs.oracle.com/javase/8/docs/api/",
          "packageListUrl": "https://docs.oracle.com/javase/8/docs/api/package-list"
        },
        {
          "url": "https://kotlinlang.org/api/latest/jvm/stdlib/",
          "packageListUrl": "https://kotlinlang.org/api/latest/jvm/stdlib/package-list"
        }
      ],
      "languageVersion": null,
      "apiVersion": null,
      "noStdlibLink": false,
      "noJdkLink": false,
      "suppressedFiles": [
        ".../src/main/kotlin/it/suppressedByPath"
      ],
      "analysisPlatform": "jvm",
      "documentedVisibilities": [
        "PUBLIC",
        "PROTECTED"
      ]
    }
  ],
  "pluginsClasspath": [
    ".../dokka-analysis-1.8.0-SNAPSHOT.jar",
    ".../dokka-base-1.8.0-SNAPSHOT.jar",
    ".../kotlin-analysis-intellij-1.8.0-SNAPSHOT.jar",
    ".../kotlin-analysis-compiler-1.8.0-SNAPSHOT.jar",
    ".../kotlinx-html-jvm-0.7.5.jar",
    ".../kotlinx-coroutines-core-jvm-1.6.3.jar",
    ".../kotlin-stdlib-jdk8-1.7.20.jar",
    ".../jackson-databind-2.12.7.1.jar",
    ".../jackson-annotations-2.12.7.jar",
    ".../jackson-core-2.12.7.jar",
    ".../jackson-module-kotlin-2.12.7.jar",
    ".../kotlin-reflect-1.7.20.jar",
    ".../kotlin-stdlib-jdk7-1.7.20.jar",
    ".../kotlin-stdlib-1.7.20.jar",
    ".../jsoup-1.15.3.jar",
    ".../freemarker-2.3.31.jar",
    ".../kotlin-stdlib-common-1.7.20.jar",
    ".../annotations-13.0.jar"
  ],
  "pluginsConfiguration": [
    {
      "fqPluginName": "org.jetbrains.dokka.base.DokkaBase",
      "serializationFormat": "JSON",
      "values": "{ \"customStyleSheets\": [\".../customResources/logo-styles.css\", \".../customResources/custom-style-to-add.css\"], \"customAssets\" : [\".../customResources/custom-resource.svg\"] }"
    }
  ],
  "modules": [],
  "failOnWarning": false,
  "delayTemplateSubstitution": false,
  "suppressObviousFunctions": false,
  "includes": [],
  "suppressInheritedMembers": false,
  "finalizeCoroutines": true
}
""".trimIndent()
)
