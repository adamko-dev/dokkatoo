import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  buildsrc.conventions.`kotlin-gradle-plugin`

  kotlin("plugin.serialization") version embeddedKotlinVersion
  `java-test-fixtures`

  `jvm-test-suite`
  `test-report-aggregation`

  buildsrc.conventions.`github-maven-publish`
}

dependencies {
  implementation("org.jetbrains.dokka:dokka-core:1.7.20")

  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
  compileOnly("com.android.tools.build:gradle:4.0.1")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

  testFixturesImplementation(gradleApi())
  testFixturesImplementation(gradleTestKit())
  testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

  val jacksonVersion = "2.12.7"
  testFixturesImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

  kotlinDokkaSource(projects.externals)

  // don't define test dependencies here, instead define them in the testing.suites {} configuration below
}

java {
  withSourcesJar()
}

gradlePlugin {
  plugins.register("dokkatoo") {
    id = "dev.adamko.dokkatoo"
    displayName = "Dokkatoo"
    description = "Generates documentation sites for Kotlin projects using Dokka"
    implementationClass = "dev.adamko.dokkatoo.DokkatooPlugin"
    isAutomatedPublishing = true
  }

  fun registerDokkaPlugin(format: String, className: String) {
    plugins.register("dokkatoo${format.toLowerCase().capitalize()}") {
      id = "dev.adamko.dokkatoo-${format.toLowerCase()}"
      displayName = "Dokkatoo $format"
      description = "Generates $format documentation sites for Kotlin projects using Dokka"
      implementationClass = "dev.adamko.dokkatoo.formats.$className"
      isAutomatedPublishing = true
    }
  }
  registerDokkaPlugin("gfm", "DokkatooGfmPlugin")
  registerDokkaPlugin("html", "DokkatooHtmlPlugin")
  registerDokkaPlugin("javadoc", "DokkatooJavadocPlugin")
  registerDokkaPlugin("jekyll", "DokkatooJekyllPlugin")
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

val projectTestMavenRepoDir = layout.buildDirectory.dir("test-maven-repo")

publishing {
  repositories {
    maven(projectTestMavenRepoDir) {
      name = "Test"
    }
  }
}


@Suppress("UnstableApiUsage") // jvm test suites are incubating
testing.suites {

  withType<JvmTestSuite>().configureEach {
    useJUnitJupiter()

    dependencies {
      implementation(project.dependencies.gradleTestKit())

      implementation("org.jetbrains.kotlin:kotlin-test:1.7.20")
      //implementation(project.dependencies.kotlin("test")) // helper function doesn't work?

      implementation(project.dependencies.platform("io.kotest:kotest-bom:5.5.5"))
      implementation("io.kotest:kotest-runner-junit5")
      implementation("io.kotest:kotest-assertions-core")
      implementation("io.kotest:kotest-assertions-json")

      implementation(project.dependencies.testFixtures(project()))

      implementation("org.jetbrains.dokka:dokka-core:1.7.20")
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
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
        dependsOn(tasks.matching { it.name == "publishAllPublicationsToTestRepository" })

        val funcTestDir = "$buildDir/functional-tests"
        systemProperties(
          "testMavenRepoDir" to file(projectTestMavenRepoDir).canonicalPath,
          "funcTestTempDir" to funcTestDir,
        )

        inputs.dir(projectTestMavenRepoDir)
        outputs.dir(funcTestDir)

        doFirst {
          File(funcTestDir).deleteRecursively()
        }
      }
    }
  }


  /** Integration tests suite */
  val testIntegration by registering(JvmTestSuite::class) {
    testType.set(TestSuiteType.INTEGRATION_TEST)

    targets.all {
      testTask.configure {
        shouldRunAfter(test, testFunctional)
        dependsOn(tasks.matching { it.name == "publishAllPublicationsToTestRepository" })

        dependsOn(project.configurations.kotlinDokkaSource)

        val integrationTestProjectsDir =
          "$rootDir/externals/kotlin-dokka/integration-tests/gradle/projects/"
        systemProperties(
          "testMavenRepoDir" to file(projectTestMavenRepoDir).canonicalPath,
          "integrationTestProjectsDir" to integrationTestProjectsDir,
        )

        inputs.dir(projectTestMavenRepoDir)
        outputs.dir(integrationTestProjectsDir)
      }
    }

//        sources {
//            java {
//                resources {
//                    srcDir(tasks.pluginUnderTestMetadata.map { it.outputDirectory })
//                }
//            }
//        }
  }

  tasks.check { dependsOn(testFunctional, testIntegration) }
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
