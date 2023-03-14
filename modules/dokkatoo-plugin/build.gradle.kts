@file:Suppress("UnstableApiUsage") // jvm test suites & test report aggregation are incubating

import buildsrc.conventions.utils.skipTestFixturesPublications
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVDefaultTask
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  buildsrc.conventions.`kotlin-gradle-plugin`

  kotlin("plugin.serialization") version embeddedKotlinVersion
  `java-test-fixtures`

  `jvm-test-suite`
  `test-report-aggregation`

  buildsrc.conventions.`github-maven-publish`

  buildsrc.conventions.`maven-publish-test`

  dev.adamko.kotlin.`binary-compatibility-validator`

  dev.adamko.`dokkatoo-html`
}

description = "Generates documentation for Kotlin projects (using Dokka)"

dependencies {

  implementation(libs.kotlin.dokkaCore)

  compileOnly(libs.gradlePlugin.kotlin)
  compileOnly(libs.gradlePlugin.android)

  implementation(platform(libs.kotlinxSerialization.bom))
  implementation(libs.kotlinxSerialization.json)

  testFixturesImplementation(gradleApi())
  testFixturesImplementation(gradleTestKit())

  testFixturesCompileOnly(libs.kotlin.dokkaCore)
  testFixturesImplementation(platform(libs.kotlinxSerialization.bom))
  testFixturesImplementation(libs.kotlinxSerialization.json)

  testFixturesCompileOnly(libs.kotlin.dokkaCore)

  testFixturesApi(platform(libs.kotest.bom))
  testFixturesApi(libs.kotest.junit5Runner)
  testFixturesApi(libs.kotest.assertionsCore)
  testFixturesApi(libs.kotest.assertionsJson)
  testFixturesApi(libs.kotest.datatest)

  // don't define test dependencies here, instead define them in the testing.suites {} configuration below
}

gradlePlugin {
  isAutomatedPublishing = true

  plugins.register("dokkatoo") {
    id = "dev.adamko.dokkatoo"
    displayName = "Dokkatoo"
    description = "Generates documentation for Kotlin projects (using Dokka)"
    implementationClass = "dev.adamko.dokkatoo.DokkatooPlugin"
  }

  fun registerDokkaPlugin(
    pluginClass: String,
    shortName: String,
    longName: String = shortName,
  ) {
    plugins.register(pluginClass) {
      id = "dev.adamko.dokkatoo-${shortName.toLowerCase()}"
      displayName = "Dokkatoo $shortName"
      description = "Generates $longName documentation for Kotlin projects (using Dokka)"
      implementationClass = "dev.adamko.dokkatoo.formats.$pluginClass"
    }
  }
  registerDokkaPlugin("DokkatooGfmPlugin", "GFM", longName = "GFM (GitHub Flavoured Markdown)")
  registerDokkaPlugin("DokkatooHtmlPlugin", "HTML")
  registerDokkaPlugin("DokkatooJavadocPlugin", "Javadoc")
  registerDokkaPlugin("DokkatooJekyllPlugin", "Jekyll")
}

pluginBundle {
  website = "https://github.com/adamko-dev/dokkatoo/"
  vcsUrl = "https://github.com/adamko-dev/dokkatoo.git"
  tags = listOf(
    "dokka",
    "kotlin",
    "kdoc",
    "android",
    "documentation",
    "javadoc",
    "html",
    "markdown",
    "gfm",
    "website"
  )
}


tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    freeCompilerArgs += listOf(
      "-opt-in=kotlin.RequiresOptIn",
      "-opt-in=dev.adamko.dokkatoo.internal.DokkatooInternalApi",
    )
  }
}

testing.suites {

  withType<JvmTestSuite>().configureEach {
    useJUnitJupiter()

    dependencies {
      implementation(project.dependencies.gradleTestKit())

      implementation(project.dependencies.testFixtures(project()))

      compileOnly(libs.kotlin.dokkaCore)

      implementation(project.dependencies.platform(libs.kotlinxSerialization.bom))
      implementation(libs.kotlinxSerialization.json)
    }

    targets.configureEach {
      testTask.configure {
        val projectTestTempDirPath = "$buildDir/test-temp-dir"
        inputs.property("projectTestTempDir", projectTestTempDirPath)
        systemProperty("projectTestTempDir", projectTestTempDirPath)

        when (testType.get()) {
          TestSuiteType.FUNCTIONAL_TEST,
          TestSuiteType.INTEGRATION_TEST -> {
            dependsOn(tasks.matching { it.name == "publishAllPublicationsToTestRepository" })

            systemProperties(
              "testMavenRepoDir" to file(mavenPublishTest.testMavenRepo).canonicalPath,
            )

            // depend on the test-publication task, but not the test-maven repo
            // (otherwise this task will never be up-to-date)
            dependsOn(tasks.publishToTestMavenRepo)
          }
        }
      }
    }
  }


  /** Unit tests suite */
  val test by getting(JvmTestSuite::class) {
    description = "Standard unit tests"
  }


  /** Functional tests suite */
  val testFunctional by registering(JvmTestSuite::class) {
    description = "Tests that use Gradle TestKit to test functionality"
    testType.set(TestSuiteType.FUNCTIONAL_TEST)

    targets.all {
      testTask.configure {
        shouldRunAfter(test)
      }
    }
  }

  tasks.check { dependsOn(test, testFunctional) }
}

skipTestFixturesPublications()

val aggregateTestReports by tasks.registering(TestReport::class) {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  destinationDirectory.set(layout.buildDirectory.dir("reports/tests/aggregated"))

  dependsOn(tasks.withType<AbstractTestTask>())

  // hardcoded dirs is a bit of a hack, but a fileTree just didn't work
  testResults.from("$buildDir/test-results/test/binary")
  testResults.from("$buildDir/test-results/testFunctional/binary")
  testResults.from("$buildDir/test-results/testIntegration/binary")

  doLast {
    logger.lifecycle("Aggregated test report: file://${destinationDirectory.asFile.get()}/index.html")
  }
}

binaryCompatibilityValidator {
  ignoredMarkers.add("dev.adamko.dokkatoo.internal.DokkatooInternalApi")

  // manually ignore all KxS serializers
  ignoredClasses.addAll(
    listOf(
      "DokkaModuleDescriptionKxs",
      "DokkaSourceSetKxs",
      "ExternalDocumentationLinkKxs",
      "PackageOptionsKxs",
      "PluginConfigurationKxs",
      "SourceLinkDefinitionKxs",
    ).map {
      "dev.adamko.dokkatoo.dokka.parameters.DokkaParametersKxs\$$it\$\$serializer"
    })
}

val dokkatooVersion = provider { project.version.toString() }


val dokkatooConstantsProperties = objects.mapProperty<String, String>().apply {
  put("DOKKATOO_VERSION", dokkatooVersion)
  put("DOKKA_VERSION", libs.versions.kotlin.dokka)
}

val buildConfigFileContents: Provider<TextResource> =
  dokkatooConstantsProperties.map { constants ->

    val vals = constants.entries
      .sortedBy { it.key }
      .joinToString("\n") { (k, v) ->
        """const val $k = "$v""""
      }.prependIndent("  ")

    resources.text.fromString(
      """
          |package dev.adamko.dokkatoo.internal
          |
          |@DokkatooInternalApi
          |object DokkatooConstants {
          |$vals
          |}
          |
        """.trimMargin()
    )
  }

val generateDokkatooConstants by tasks.registering(Sync::class) {

  val buildConfigFileContents = buildConfigFileContents

  from(buildConfigFileContents) {
    rename { "DokkatooConstants.kt" }
    into("dev/adamko/dokkatoo/internal/")
  }

  into(layout.buildDirectory.dir("generated-source/main/kotlin/"))
}

kotlin.sourceSets.main {
  kotlin.srcDir(generateDokkatooConstants.map { it.destinationDir })
}

dokkatoo {
  // create a special source set just for documenting the internally visible DokkatooInternalApi
  dokkatooSourceSets.create("DokkatooInternalApi") {
    documentedVisibilities(DokkaConfiguration.Visibility.INTERNAL)
    suppress.set(false)
    sourceRoots.from(layout.projectDirectory.dir("src/main/kotlin").asFileTree.matching {
      include("**/DokkatooInternalApi.kt")
    })
  }
}
