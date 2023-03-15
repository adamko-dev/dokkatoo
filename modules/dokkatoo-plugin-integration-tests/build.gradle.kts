@file:Suppress("UnstableApiUsage") // jvm test suites & test report aggregation are incubating

import buildsrc.conventions.utils.skipTestFixturesPublications
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") //version embeddedKotlinVersion
  kotlin("plugin.serialization") version embeddedKotlinVersion
  `java-test-fixtures`

  `jvm-test-suite`
  `test-report-aggregation`

  buildsrc.conventions.`java-base`
  buildsrc.conventions.`maven-publish-test`
  buildsrc.conventions.`dokkatoo-example-projects`
}

description = """
    Integration tests for Dokkatoo Gradle Plugin. 
    The tests use Gradle TestKit to run the template projects that are committed in the repo.
  """.trimIndent()

dependencies {
  testMavenPublication(projects.modules.dokkatooPlugin)
  exampleProjects(projects.examples)

  testFixturesApi(testFixtures(projects.modules.dokkatooPlugin))

  testFixturesImplementation(gradleTestKit())

  testFixturesImplementation(platform(libs.kotlinxSerialization.bom))
  testFixturesImplementation(libs.kotlinxSerialization.json)

  testFixturesCompileOnly(libs.kotlin.dokkaCore)

  // don't define test dependencies here, instead define them in the testing.suites {} configuration below
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

        // depend on the test-publication configuration, but not the test-maven repo dir
        // (otherwise this task will never be up-to-date)
        dependsOn(configurations.testMavenPublication)

        // depend on example & integration-test projects setup
        dependsOn(configurations.exampleProjects)
        dependsOn(tasks.updateDokkatooExamples)

        val dokkatooExamplesDir = configurations.exampleProjects.map {
          it.incoming.files.singleFile.absolutePath
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
    description = "Test the example projects, from the 'examples' directory in the project root"
  }


  /** Integration tests suite */
  val testIntegration by registering(JvmTestSuite::class) {
    description =
      "Test the integration template projects, in the dokkatoo-plugin-integration-tests/projects directory"
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

skipTestFixturesPublications()

tasks.setupDokkaTemplateProjects {
  destinationToSources.set(
    mapOf(
      //@formatter:off
      "projects/it-android-0/dokka"                to "integration-tests/gradle/projects/it-android-0",
      "projects/it-basic/dokka"                    to "integration-tests/gradle/projects/it-basic",
      "projects/it-basic-groovy/dokka"             to "integration-tests/gradle/projects/it-basic-groovy",
      "projects/it-collector-0/dokka"              to "integration-tests/gradle/projects/it-collector-0",
      "projects/it-js-ir-0/dokka"                  to "integration-tests/gradle/projects/it-js-ir-0",
      "projects/it-multimodule-0/dokka"            to "integration-tests/gradle/projects/it-multimodule-0",
      "projects/it-multimodule-1/dokka"            to "integration-tests/gradle/projects/it-multimodule-1",
      "projects/it-multimodule-versioning-0/dokka" to "integration-tests/gradle/projects/it-multimodule-versioning-0",
      "projects/it-multiplatform-0/dokka"          to "integration-tests/gradle/projects/it-multiplatform-0",

      //"integration-tests/gradle/projects/coroutines"                  to "projects/coroutines/dokka",
      //"integration-tests/gradle/projects/serialization"               to "projects/serialization/dokka",
      //"integration-tests/gradle/projects/stdlib"                      to "projects/stdlib/dokka",
      //@formatter:on
    ).entries.associate { (dest, rootDir) ->
      projectDir.resolve(dest) to listOf(
        rootDir,
        "integration-tests/gradle/projects/template.root.gradle.kts",
        "integration-tests/gradle/projects/template.settings.gradle.kts",
      )
    }
  )
}

tasks.withType<Test>().configureEach {
  // this seems to help OOM errors in the Worker Daemons
  //setForkEvery(1)
  jvmArgs(
    "-Xmx1g",
    "-XX:MaxMetaspaceSize=512m",
    "-XX:+AlwaysPreTouch", // https://github.com/gradle/gradle/issues/3093#issuecomment-387259298
  )
}

dokkaSourceDownload {
  dokkaVersion.set(libs.versions.kotlin.dokka)
}
