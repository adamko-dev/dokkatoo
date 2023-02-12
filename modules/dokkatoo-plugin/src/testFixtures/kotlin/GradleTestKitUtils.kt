package dev.adamko.dokkatoo.utils

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language


// utils for testing using Gradle TestKit


class GradleProjectTest(
  val projectDir: Path,
) {
  constructor(
    testProjectName: String,
    baseDir: Path = funcTestTempDir,
  ) : this(
    projectDir = baseDir.resolve(testProjectName),
  )

  val runner: GradleRunner = GradleRunner.create().withProjectDir(projectDir.toFile())

  val testMavenRepoRelativePath: String =
    projectDir.relativize(testMavenRepoDir).toFile().invariantSeparatorsPath

  fun createFile(filePath: String, contents: String): File =
    projectDir.resolve(filePath).toFile().apply {
      parentFile.mkdirs()
      createNewFile()
      writeText(contents)
    }

  companion object {

    /** file-based Maven Repo that contains the Dokka dependencies */
    val testMavenRepoDir: Path by systemProperty(Paths::get)

    val projectTestTempDir: Path by systemProperty(Paths::get)

    /** Temporary directory for the functional tests. This directory will be auto-deleted. */
    val funcTestTempDir: Path by lazy {
      projectTestTempDir.resolve("functional-tests")
    }

    private val dokkaSourceDir: Path by systemProperty(Paths::get)
    /** Directory that contains projects used for integration tests */
    val integrationTestProjectsDir: Path by lazy {
      dokkaSourceDir.resolve("integration-tests/gradle/projects")
    }

    private fun <T> systemProperty(
      convert: (String) -> T,
    ) = ReadOnlyProperty<Any, T> { _, property ->
      val value = requireNotNull(System.getProperty(property.name)) {
        "system property ${property.name} is unavailable"
      }
      convert(value)
    }
  }
}


/**
 * Load a project from the [GradleProjectTest.integrationTestProjectsDir]
 */
fun gradleKtsProjectIntegrationTest(
  testProjectName: String,
  build: GradleProjectTest.() -> Unit,
): GradleProjectTest =
  GradleProjectTest(
    baseDir = GradleProjectTest.integrationTestProjectsDir,
    testProjectName = testProjectName,
  ).apply(build)


/**
 * Builder for testing a Gradle project that uses Kotlin script DSL and creates default
 * `settings.gradle.kts` and `gradle.properties` files.
 *
 * @param[testProjectName] the path of the project directory, relative to [baseDir
 */
fun gradleKtsProjectTest(
  testProjectName: String,
  baseDir: Path = GradleProjectTest.funcTestTempDir,
  build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
  return GradleProjectTest(baseDir = baseDir, testProjectName = testProjectName).apply {

    settingsGradleKts = """
            |rootProject.name = "test"
            |
            |@Suppress("UnstableApiUsage") // Central declaration of repositories is an incubating feature
            |dependencyResolutionManagement {
            |    repositories {
            |        mavenCentral()
            |        maven(file("$testMavenRepoRelativePath"))
            |    }
            |}
            |
            |pluginManagement {
            |    repositories {
            |        gradlePluginPortal()
            |        mavenCentral()
            |        maven(file("$testMavenRepoRelativePath"))
            |    }
            |}
            |
        """.trimMargin()

    gradleProperties = """
            |kotlin.mpp.stability.nowarn=true
            |org.gradle.cache=true
       """.trimMargin()

    build()
  }
}

/**
 * Builder for testing a Gradle project that uses Groovy script and creates default,
 * `settings.gradle`, and `gradle.properties` files.
 *
 * @param[testProjectName] the name of the test, which should be distinct across the project
 */
fun gradleGroovyProjectTest(
  testProjectName: String,
  baseDir: Path = GradleProjectTest.funcTestTempDir,
  build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
  return GradleProjectTest(baseDir = baseDir, testProjectName = testProjectName).apply {

    settingsGradle = """
            |rootProject.name = "test"
            |
            |dependencyResolutionManagement {
            |    repositories {
            |        mavenCentral()
            |        maven { url = file("$testMavenRepoRelativePath") }
            |    }
            |}
            |
            |pluginManagement {
            |    repositories {
            |        gradlePluginPortal()
            |        mavenCentral()
            |        maven { url = file("$testMavenRepoRelativePath") }
            |    }
            |}
            |
        """.trimMargin()

    gradleProperties = """
            |kotlin.mpp.stability.nowarn=true
            |org.gradle.cache=true
       """.trimMargin()

    build()
  }
}


fun GradleProjectTest.projectFile(
  @Language("TEXT")
  filePath: String
): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, String>> =
  PropertyDelegateProvider { _, _ ->
    TestProjectFileProvidedDelegate(this, filePath)
  }


/** Delegate for reading and writing a [GradleProjectTest] file. */
private class TestProjectFileProvidedDelegate(
  private val project: GradleProjectTest,
  private val filePath: String,
) : ReadWriteProperty<Any?, String> {
  override fun getValue(thisRef: Any?, property: KProperty<*>): String =
    project.projectDir.resolve(filePath).toFile().readText()

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    project.createFile(filePath, value)
  }
}

/** Delegate for reading and writing a [GradleProjectTest] file. */
private class TestProjectFileDelegate(
  private val filePath: String,
) : ReadWriteProperty<GradleProjectTest, String> {
  override fun getValue(thisRef: GradleProjectTest, property: KProperty<*>): String =
    thisRef.projectDir.resolve(filePath).toFile().readText()

  override fun setValue(thisRef: GradleProjectTest, property: KProperty<*>, value: String) {
    thisRef.createFile(filePath, value)
  }
}


/** Set the content of `settings.gradle.kts` */
@delegate:Language("kts")
var GradleProjectTest.settingsGradleKts: String by TestProjectFileDelegate("settings.gradle.kts")


/** Set the content of `build.gradle.kts` */
@delegate:Language("kts")
var GradleProjectTest.buildGradleKts: String by TestProjectFileDelegate("build.gradle.kts")


/** Set the content of `settings.gradle` */
@delegate:Language("groovy")
var GradleProjectTest.settingsGradle: String by TestProjectFileDelegate("settings.gradle")


/** Set the content of `build.gradle` */
@delegate:Language("groovy")
var GradleProjectTest.buildGradle: String by TestProjectFileDelegate("build.gradle")


/** Set the content of `gradle.properties` */
@delegate:Language("properties")
var GradleProjectTest.gradleProperties: String by TestProjectFileDelegate("gradle.properties")

fun GradleProjectTest.createKotlinFile(filePath: String, @Language("kotlin") contents: String) =
  createFile(filePath, contents)

fun GradleProjectTest.createKtsFile(filePath: String, @Language("kts") contents: String) =
  createFile(filePath, contents)


fun GradleRunner.withEnvironment(vararg map: Pair<String, String>): GradleRunner =
  withEnvironment(map.toMap())
