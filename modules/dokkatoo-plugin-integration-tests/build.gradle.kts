@file:Suppress("UnstableApiUsage") // jvm test suites & test report aggregation are incubating

import buildsrc.tasks.SetupDokkaProjects
import buildsrc.utils.buildDir_
import buildsrc.utils.skipTestFixturesPublications
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  `java-test-fixtures`

  `jvm-test-suite`
  `test-report-aggregation`

  buildsrc.conventions.`java-base`
  buildsrc.conventions.`maven-publish-test`
  buildsrc.conventions.`dokkatoo-example-projects`
  buildsrc.conventions.`android-setup`
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

kotlin {
  sourceSets {
    configureEach {
      compilerOptions {
        optIn.addAll(
          "dev.adamko.dokkatoo.internal.DokkatooInternalApi",
          "kotlin.io.path.ExperimentalPathApi",
        )
      }
    }
  }
}

//region Test suites and task configuration
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
        javaLauncher.set(javaToolchains.launcherFor {
          // Android test project requires Java 17
          languageVersion.set(JavaLanguageVersion.of(17))
        })

        val projectTestTempDirPath = "$buildDir_/test-temp-dir"
        inputs.property("projectTestTempDir", projectTestTempDirPath)
        systemProperty("projectTestTempDir", projectTestTempDirPath)

        val exampleProjectDataPath = layout.projectDirectory.dir("example-project-data")
          .asFile.invariantSeparatorsPath
        systemProperty("exampleProjectDataPath", exampleProjectDataPath)

        // depend on the test-publication configuration, but not the test-maven repo dir
        // (otherwise this task will never be up-to-date)
        dependsOn(configurations.testMavenPublicationResolvable)

        // depend on example & integration-test projects setup
        dependsOn(configurations.exampleProjectsResolvable)
        dependsOn(tasks.updateDokkatooExamples)

        val dokkatooExamplesDir = configurations.exampleProjectsResolvable.map {
          it.incoming.files.singleFile.absolutePath
        }

        systemProperty("integrationTestProjectsDir", "$projectDir/projects")
        systemProperty("testMavenRepoDir", file(mavenPublishTest.testMavenRepo).canonicalPath)
        doFirst {
          // workaround for lazy-properties not working https://github.com/gradle/gradle/issues/12247
          systemProperty("exampleProjectsDir", dokkatooExamplesDir.get())
        }

        systemProperty(
          "kotest.framework.config.fqn",
          "dev.adamko.dokkatoo.utils.KotestProjectConfig",
        )
        // FIXME remove autoscan when Kotest >= 6.0
        systemProperty("kotest.framework.classpath.scanning.autoscan.disable", "true")
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
  // this seems to help OOM errors in the Worker Daemons
  //setForkEvery(1)
  jvmArgs(
    "-Xmx1g",
    "-XX:MaxMetaspaceSize=512m",
  )

  mustRunAfter(tasks.withType<AbstractPublishToMaven>())
}
//endregion

//region Example & Template projects setup
dokkatooExampleProjects {
  exampleProjects {
    projectsItAndroid0Dokkatoo {
      gradlePropertiesContent.add("android.useAndroidX=true")
    }
  }
}

dokkaTemplateProjects {

  val androidLocalPropertiesFile = tasks.createAndroidLocalPropertiesFile.map {
    it.outputs.files
  }

  register(
    source = "dokka-integration-tests/gradle/projects/it-android-0",
    destination = "projects/it-android-0/dokka",
  ) {
    additionalFiles.from(androidLocalPropertiesFile)
  }
  register(
    source = "dokka-integration-tests/gradle/projects/it-basic",
    destination = "projects/it-basic/dokka",
  )
  register(
    source = "dokka-integration-tests/gradle/projects/it-basic-groovy",
    destination = "projects/it-basic-groovy/dokka",
  )
  register(
    source = "dokka-integration-tests/gradle/projects/it-collector-0",
    destination = "projects/it-collector-0/dokka",
  )
  register(
    source = "dokka-integration-tests/gradle/projects/it-js-ir-0",
    destination = "projects/it-js-ir-0/dokka",
  )
  register(
    source = "dokka-integration-tests/gradle/projects/it-multimodule-0",
    destination = "projects/it-multimodule-0/dokka",
  )
  register(
    source = "dokka-integration-tests/gradle/projects/it-multimodule-1",
    destination = "projects/it-multimodule-1/dokka",
  )
  register(
    source = "dokka-integration-tests/gradle/projects/it-multimodule-versioning-0",
    destination = "projects/it-multimodule-versioning-0/dokka",
  )
  register(
    source = "dokka-integration-tests/gradle/projects/it-multiplatform-0",
    destination = "projects/it-multiplatform-0/dokka",
  )

//    register("projects/coroutines/dokka") { }
//    register("projects/serialization/dokka") { }
//    register("projects/stdlib/dokka") { }

  configureEach {
    additionalPaths.addAll(
      "dokka-integration-tests/gradle/projects/template.root.gradle.kts",
      "dokka-integration-tests/gradle/projects/template.settings.gradle.kts",
    )
  }
}

tasks.setupDokkaTemplateProjects.configure {

  val kotlinDokkaVersion = libs.versions.kotlin.dokka
  inputs.property("kotlinDokkaVersion", kotlinDokkaVersion)

  doLast {
    outputs.files.asFileTree.files.forEach { file ->
      when (file.name) {
        "build.gradle.kts" -> {
          file.writeText(
            file.readText()
              .replace(
                """../template.root.gradle.kts""",
                """./template.root.gradle.kts""",
              ).replace(
                """${'$'}{System.getenv("DOKKA_VERSION")}""",
                kotlinDokkaVersion.get(),
              )
          )
        }

        "settings.gradle.kts" -> {
          file.writeText(
            file.readText()
              .replace(
                """../template.settings.gradle.kts""",
                """./template.settings.gradle.kts""",
              )
          )
        }

        "template.settings.gradle.kts" -> {
          file.writeText(
            file.readText()
              .replace(
                """${'$'}{System.getenv("DOKKA_VERSION")}""",
                kotlinDokkaVersion.get(),
              )
              .replace(
                """val dokka_it_dokka_version: String by settings""",
                """val dokka_it_dokka_version: String = "${kotlinDokkaVersion.get()}"""",
              )
              .let { str ->
                // replace mavenLocal {} and everything inside the brackets
                val mavenLocalStart = "mavenLocal {"
                val start = str.indexOf(mavenLocalStart)
                var end = start + mavenLocalStart.length
                var braces = 1
                while (braces > 0) {
                  val c = str.getOrNull(++end) ?: break
                  if (c == '{') braces++
                  if (c == '}') braces--
                }
                str.removeRange(start..end)
              }
          )
        }
      }
    }
  }
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

tasks.updateAndroidLocalProperties {
  mustRunAfter(tasks.withType<SetupDokkaProjects>())
}

tasks.updateDokkatooExamples {
  dependsOn(tasks.updateAndroidLocalProperties)
}
//endregion

skipTestFixturesPublications()
