@file:Suppress("UnstableApiUsage") // jvm test suites & test report aggregation are incubating

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  buildsrc.conventions.`kotlin-gradle-plugin`

  kotlin("plugin.serialization") version embeddedKotlinVersion
  `java-test-fixtures`

  `jvm-test-suite`
  `test-report-aggregation`

  buildsrc.conventions.`github-maven-publish`

  buildsrc.conventions.`maven-publish-test`
}

dependencies {
  implementation("org.jetbrains.dokka:dokka-core:1.7.20")

  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
  compileOnly("com.android.tools.build:gradle:4.0.1")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

  testFixturesImplementation(gradleApi())
  testFixturesImplementation(gradleTestKit())
  testFixturesCompileOnly("org.jetbrains.dokka:dokka-core:1.7.20")
  testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
  testFixturesApi(platform("io.kotest:kotest-bom:5.5.5"))
  testFixturesApi("io.kotest:kotest-runner-junit5")
  testFixturesApi("io.kotest:kotest-assertions-core")
  testFixturesApi("io.kotest:kotest-assertions-json")
  testFixturesApi("io.kotest:kotest-framework-datatest")

//  kotlinDokkaSource(projects.externals)

  // don't define test dependencies here, instead define them in the testing.suites {} configuration below
}

gradlePlugin {
  isAutomatedPublishing = true

  plugins.register("dokkatoo") {
    id = "dev.adamko.dokkatoo"
    displayName = "Dokkatoo"
    description = "Generates documentation sites for Kotlin projects using Dokka"
    implementationClass = "dev.adamko.dokkatoo.DokkatooPlugin"
  }

  fun registerDokkaPlugin(
    className: String,
    shortName: String,
    longName: String = shortName,
  ) {
    plugins.register(className) {
      id = "dev.adamko.dokkatoo-${shortName.toLowerCase()}"
      displayName = "Dokkatoo $shortName"
      description = "Generates $longName documentation sites for Kotlin projects using Dokka"
      implementationClass = "dev.adamko.dokkatoo.formats.$className"
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
    this.freeCompilerArgs += listOf(
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

      implementation(project.dependencies.testFixtures(project))

      implementation("org.jetbrains.dokka:dokka-core:1.7.20")
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
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
  val test by getting(JvmTestSuite::class)


  /** Functional tests suite */
  val testFunctional by registering(JvmTestSuite::class) {
    testType.set(TestSuiteType.FUNCTIONAL_TEST)

    targets.all {
      testTask.configure {
        shouldRunAfter(test)
      }
    }
  }

  tasks.check { dependsOn(testFunctional) }
}


tasks.withType<Test>().configureEach {

  mustRunAfter(tasks.withType<AbstractPublishToMaven>())

  testLogging {
    events = setOf(
      TestLogEvent.STARTED,
      TestLogEvent.PASSED,
      TestLogEvent.SKIPPED,
      TestLogEvent.FAILED,
      TestLogEvent.STANDARD_OUT,
      TestLogEvent.STANDARD_ERROR,
    )
    showStandardStreams = true
    showExceptions = true
    showCauses = true
    showStackTraces = true
  }
}


// don't publish test fixtures (which causes warnings when publishing)
// https://docs.gradle.org/current/userguide/java_testing.html#publishing_test_fixtures
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }


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
