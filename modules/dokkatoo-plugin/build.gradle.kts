@file:Suppress("UnstableApiUsage") // jvm test suites & test report aggregation are incubating

import buildsrc.conventions.utils.skipTestFixturesPublications
import org.gradle.kotlin.dsl.support.serviceOf
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
}

description = "Generates documentation for Kotlin projects (using Dokka)"



val gradleApiDl by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
  isVisible = true
}

dependencies {
  gradleApiDl("distributions:gradle:8.0.2")
}

val libDir = layout.projectDirectory.dir("lib")

val downloadGradleApi by tasks.registering(Sync::class) {
  group = "plugin development"

  val archives = serviceOf<ArchiveOperations>()

  from(gradleApiDl.incoming.artifacts.resolvedArtifacts.map { artifacts ->
    val gradleDistZip = artifacts.single().file
    val gradleDist = archives.zipTree(gradleDistZip)
    gradleDist
      .matching { include("**/gradle-core-api*jar") }
      .files
  }) {
    rename { "gradle-core-api.jar" }
  }
  into(libDir)
}

dependencies {
  implementation("org.jetbrains.dokka:dokka-core:1.7.20")

  compileOnly(libDir.files("gradle-core-api.jar"))
  implementation(kotlin("stdlib"))

  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
  compileOnly("com.android.tools.build:gradle:4.0.1")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

  testFixturesImplementation(libDir.files("gradle-core-api.jar"))
//  testFixturesImplementation(gradleApi())
  testFixturesImplementation(gradleTestKit())
  testFixturesCompileOnly("org.jetbrains.dokka:dokka-core:1.7.20")
  testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
  testFixturesApi(platform("io.kotest:kotest-bom:5.5.5"))
  testFixturesApi("io.kotest:kotest-runner-junit5")
  testFixturesApi("io.kotest:kotest-assertions-core")
  testFixturesApi("io.kotest:kotest-assertions-json")
  testFixturesApi("io.kotest:kotest-framework-datatest")

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

      implementation("org.jetbrains.kotlin:kotlin-test:1.7.20")

      implementation(project.dependencies.testFixtures(project()))

      implementation("org.jetbrains.dokka:dokka-core:1.7.20")
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

      runtimeOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion")
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
