@file:Suppress("UnstableApiUsage") // jvm test suites & test report aggregation are incubating

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") //version embeddedKotlinVersion
  kotlin("plugin.serialization") version embeddedKotlinVersion
  `java-test-fixtures`

  `jvm-test-suite`
  `test-report-aggregation`

  buildsrc.conventions.`java-base`
  buildsrc.conventions.`dokka-source-downloader`
  buildsrc.conventions.`maven-publish-test`
  buildsrc.conventions.`dokkatoo-example-projects`
}

description = "Integration tests for Dokkatoo Gradle Plugin. The tests use Gradle TestKit to run the template projects that are committed in the repo."

dependencies {
  testMavenPublication(projects.modules.dokkatooPlugin)
  exampleProjects(projects.examples)

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

  testFixturesApi(testFixtures(projects.modules.dokkatooPlugin))
  testFixturesImplementation(gradleTestKit())
  testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
  testFixturesImplementation(platform("io.kotest:kotest-bom:5.5.5"))
  testFixturesImplementation("io.kotest:kotest-runner-junit5")
  testFixturesImplementation("io.kotest:kotest-assertions-core")
  testFixturesImplementation("io.kotest:kotest-assertions-json")

  val jacksonVersion = "2.12.7"
  testFixturesImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

//  kotlinDokkaSource(projects.externals)

  // don't define test dependencies here, instead define them in the testing.suites {} configuration below
}


tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    this.freeCompilerArgs += listOf(
      "-opt-in=kotlin.RequiresOptIn",
      //"-opt-in=dev.adamko.dokkatoo.internal.DokkatooInternalApi",
    )
  }
}

testing.suites {

  withType<JvmTestSuite>().configureEach {
    useJUnitJupiter()

    dependencies {
      implementation(project.dependencies.gradleTestKit())

      implementation("org.jetbrains.kotlin:kotlin-test:1.7.20")

      implementation(project.dependencies.platform("io.kotest:kotest-bom:5.5.5"))
      implementation("io.kotest:kotest-runner-junit5")
      implementation("io.kotest:kotest-assertions-core")
      implementation("io.kotest:kotest-assertions-json")

      implementation(project.dependencies.testFixtures(project()))

      implementation("org.jetbrains.dokka:dokka-core:1.7.20")
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    }

    targets.configureEach {
      testTask.configure {
        val projectTestTempDirPath = "$buildDir/test-temp-dir"
        inputs.property("projectTestTempDir", projectTestTempDirPath)
        systemProperty("projectTestTempDir", projectTestTempDirPath)

        // depend on the test-publication configuration, but not the test-maven repo dir
        // (otherwise this task will never be up-to-date)
        dependsOn(configurations.testMavenPublication)

        // depend on example & integration-test projects setup
        dependsOn(configurations.exampleProjects)
        dependsOn(tasks.setupDokkaTemplateProjects)
        dependsOn(tasks.updateGradlePropertiesInDokkatooExamples)

        val dokkatooExamplesDir = configurations.exampleProjects.map {
          it.incoming.artifactView { lenient(true) }.files.singleFile.absolutePath
        }

        systemProperty("integrationTestProjectsDir", "$projectDir/projects")
        systemProperty("testMavenRepoDir", file(mavenPublishTest.testMavenRepo).canonicalPath)
        doFirst {
          // workaround for lazy-properties not working https://github.com/gradle/gradle/issues/12247
          systemProperty("exampleProjectsDir", dokkatooExamplesDir.get())
        }
      }
    }
  }


  /** Examples tests suite */
  val testExamples by registering(JvmTestSuite::class) {
//    testType.set(TestSuiteType.FUNCTIONAL_TEST)

    targets.all {
      testTask.configure {
//        dependsOn(project.configurations.kotlinDokkaSource)

//        inputs.property("dokkaSourceDir",
//          project.configurations.kotlinDokkaSource.map { dokkaSrcConf ->
//            val files = dokkaSrcConf.incoming.artifactView { lenient(true) }.files
//            files.singleOrNull()?.absolutePath
//              ?: error("could not get Dokka source code directory from kotlinDokkaSource configuration. Got ${files.count()} files: $files")
//          }
//        )
//
//        systemProperty("dokkaSourceDir", inputs.properties["dokkaSourceDir"]!!)
      }
    }
  }


  /** Integration tests suite */
  val testIntegration by registering(JvmTestSuite::class) {
//    testType.set(TestSuiteType.INTEGRATION_TEST)

    targets.all {
      testTask.configure {
//        dependsOn(project.configurations.kotlinDokkaSource)

//        inputs.property("dokkaSourceDir",
//          project.configurations.kotlinDokkaSource.map { dokkaSrcConf ->
//            val files = dokkaSrcConf.incoming.artifactView { lenient(true) }.files
//            files.singleOrNull()?.absolutePath
//              ?: error("could not get Dokka source code directory from kotlinDokkaSource configuration. Got ${files.count()} files: $files")
//          }
//        )
//
//        systemProperty("dokkaSourceDir", inputs.properties["dokkaSourceDir"]!!)
      }
    }
  }

  tasks.check { dependsOn(testExamples, testIntegration) }
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

tasks.setupDokkaTemplateProjects {
  destinationToSources.set(
    mapOf(
      //@formatter:off
      "projects/it-android-0/dokka"                to listOf("integration-tests/gradle/projects/it-android-0"),
      "projects/it-basic/dokka"                    to listOf("integration-tests/gradle/projects/it-basic"),
      "projects/it-basic-groovy/dokka"             to listOf("integration-tests/gradle/projects/it-basic-groovy"),
      "projects/it-collector-0/dokka"              to listOf("integration-tests/gradle/projects/it-collector-0"),
      "projects/it-js-ir-0/dokka"                  to listOf("integration-tests/gradle/projects/it-js-ir-0"),
      "projects/it-multimodule-0/dokka"            to listOf("integration-tests/gradle/projects/it-multimodule-0"),
      "projects/it-multimodule-1/dokka"            to listOf("integration-tests/gradle/projects/it-multimodule-1"),
      "projects/it-multimodule-versioning-0/dokka" to listOf("integration-tests/gradle/projects/it-multimodule-versioning-0"),
      "projects/it-multiplatform-0/dokka"          to listOf("integration-tests/gradle/projects/it-multiplatform-0"),

      //"integration-tests/gradle/projects/coroutines"                  to "projects/coroutines/dokka",
      //"integration-tests/gradle/projects/serialization"               to "projects/serialization/dokka",
      //"integration-tests/gradle/projects/stdlib"                      to "projects/stdlib/dokka",
      //@formatter:on
    ).mapKeys { (dest, _) ->
      projectDir.resolve(dest)
    }.mapValues { (_, sources) ->
      sources + listOf(
        "integration-tests/gradle/projects/template.root.gradle.kts",
        "integration-tests/gradle/projects/template.settings.gradle.kts",
      )
    }
  )
}

tasks.withType<Test>().configureEach {
  // this seems to help OOM errors in the Worker Daemons
  setForkEvery(1)
  jvmArgs(
    "-Xmx1g",
    //"-XX:MaxMetaspaceSize=512m",
  )
}
