@file:Suppress("UnstableApiUsage") // jvm test suites & test report aggregation are incubating

import buildsrc.tasks.GenerateDokkatooConstants
import buildsrc.utils.buildDir_
import buildsrc.utils.skipTestFixturesPublications

plugins {
  buildsrc.conventions.`kotlin-gradle-plugin`
  kotlin("plugin.serialization")

  dev.adamko.kotlin.`binary-compatibility-validator`

  buildsrc.conventions.dokkatoo
  buildsrc.conventions.`maven-publishing`

  `java-test-fixtures`
  `jvm-test-suite`
  `test-report-aggregation`
  buildsrc.conventions.`maven-publish-test`

  buildsrc.conventions.`dokka-source-downloader`
}

description = "Generates documentation for Kotlin projects (using Dokka)"

dependencies {
  // ideally there should be a 'dokka-core-api' dependency (that is very thin and doesn't drag in loads of unnecessary code)
  // that would be used as an implementation dependency, while dokka-core would be used as a compileOnly dependency
  // https://github.com/Kotlin/dokka/issues/2933
  implementation(libs.kotlin.dokkaCore)

  compileOnly(libs.gradlePlugin.kotlin)
  compileOnly(libs.gradlePlugin.kotlin.klibCommonizerApi)
  compileOnly(libs.gradlePlugin.android)
  compileOnly(libs.gradlePlugin.androidApi)

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
      id = "dev.adamko.dokkatoo-${shortName.lowercase()}"
      displayName = "Dokkatoo $shortName"
      description = "Generates $longName documentation for Kotlin projects (using Dokka)"
      implementationClass = "dev.adamko.dokkatoo.formats.$pluginClass"
    }
  }
  registerDokkaPlugin("DokkatooGfmPlugin", "GFM", longName = "GFM (GitHub Flavoured Markdown)")
  registerDokkaPlugin("DokkatooHtmlPlugin", "HTML")
  registerDokkaPlugin("DokkatooJavadocPlugin", "Javadoc")
  registerDokkaPlugin("DokkatooJekyllPlugin", "Jekyll")

  plugins.configureEach {
    website.set("https://adamko-dev.github.io/dokkatoo/")
    vcsUrl.set("https://github.com/adamko-dev/dokkatoo.git")
    tags.addAll(
      "dokka",
      "dokkatoo",
      "kotlin",
      "kdoc",
      "android",
      "api reference",
      "documentation",
      "javadoc",
      "html",
      "markdown",
      "gfm",
      "website",
    )
  }
}

kotlin {
  sourceSets.configureEach {
    languageSettings {
      optIn("dev.adamko.dokkatoo.internal.DokkatooInternalApi")
      optIn("kotlin.io.path.ExperimentalPathApi")
    }
  }
}

testing.suites {
  withType<JvmTestSuite>().configureEach {
    useJUnitJupiter()

    dependencies {
      implementation(project.dependencies.gradleTestKit())

      implementation(project.dependencies.testFixtures(project()))

      implementation(project.dependencies.platform(libs.kotlinxSerialization.bom))
      implementation(libs.kotlinxSerialization.json)
    }

    targets.configureEach {
      testTask.configure {
        val projectTestTempDirPath = "$buildDir_/test-temp-dir"
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

    dependencies {
      implementation(project.dependencies.platform(libs.ktor.bom))
      implementation(libs.ktorServer.core)
      implementation(libs.ktorServer.cio)
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
  testResults.from("$buildDir_/test-results/test/binary")
  testResults.from("$buildDir_/test-results/testFunctional/binary")
  testResults.from("$buildDir_/test-results/testIntegration/binary")

  doLast {
    logger.lifecycle("Aggregated test report: file://${destinationDirectory.asFile.get()}/index.html")
  }
}

binaryCompatibilityValidator {
  ignoredMarkers.add("dev.adamko.dokkatoo.internal.DokkatooInternalApi")
}

val dokkatooVersion = provider { project.version.toString() }

val dokkatooConstantsProperties = objects.mapProperty<String, String>().apply {
  put("DOKKATOO_VERSION", dokkatooVersion)
  put("DOKKA_VERSION", libs.versions.kotlin.dokka)
}

val generateDokkatooConstants by tasks.registering(GenerateDokkatooConstants::class) {
  properties = dokkatooConstantsProperties
  destinationDir.set(layout.buildDirectory.dir("generated-source/main/kotlin/"))
  dokkaSource.fileProvider(tasks.prepareDokkaSource.map { it.destinationDir })
}

kotlin.sourceSets.main {
  kotlin.srcDir(generateDokkatooConstants.map { it.destinationDir })
}

dokkatoo {
  moduleName = "Dokkatoo Gradle Plugin"

  dokkatooSourceSets.configureEach {
    includes.from("Module.md")
  }
}

  pluginsConfiguration {
    html {
      homepageLink = "https://github.com/adamko-dev/dokkatoo/"
    }
  }
}

dokkaSourceDownload {
  dokkaVersion.set(libs.versions.kotlin.dokka)
}
