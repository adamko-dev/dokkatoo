package buildsrc.conventions

import buildsrc.tasks.SetupDokkaProjects
import java.io.File
import java.time.Duration
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import buildsrc.utils.skipTestFixturesPublications
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.util.suffixIfNot


/**
 * Utilities for preparing Android projects
 */

plugins {
  base
  id("buildsrc.conventions.base")
}


val androidSdkDir: Provider<File> = providers
  // first try getting the SDK installed on via GitHub step setup-android
  .environmentVariable("ANDROID_SDK_ROOT").map(::File)
  // else get the project-local SDK
  .orElse(layout.projectDirectory.file("projects/ANDROID_SDK").asFile)



val createAndroidLocalPropertiesFile by tasks.registering {

  val localPropertiesFile = temporaryDir.resolve("local.properties")
  outputs.file(localPropertiesFile).withPropertyName("localPropertiesFile")

  val androidSdkDir = androidSdkDir
  // add the relative path as a property for Gradle up-to-date checks (the directory contents don't matter)
  inputs.property(
    "androidSdkDirPath",
    androidSdkDir.map { it.relativeTo(projectDir).invariantSeparatorsPath }
  )

  doLast {
    val androidSdkPath = androidSdkDir.get().invariantSeparatorsPath

    localPropertiesFile.apply {
      parentFile.mkdirs()
      createNewFile()
      writeText(
        """
          |# DO NOT EDIT - Generated by $path
          |
          |sdk.dir=$androidSdkPath
          |
        """.trimMargin()
      )
    }
  }
}


val updateAndroidLocalProperties by tasks.registering {

  // find all local.properties files
  val localPropertiesFiles = layout.projectDirectory.dir("projects").asFileTree
    .matching {
      include("**/local.properties")
    }.files

  outputs.files(localPropertiesFiles).withPropertyName("localPropertiesFiles")

  val androidSdkDir = androidSdkDir

  // add the relative path as a property for Gradle up-to-date checks (the directory contents don't matter)
  inputs.property(
    "androidSdkDirPath",
    androidSdkDir.map { it.relativeTo(projectDir).invariantSeparatorsPath }
  )

  doLast {
    val androidSdkDirPath = androidSdkDir.get().invariantSeparatorsPath

    localPropertiesFiles.forEach { file ->
      if (file.exists()) {
        file.writeText(
          file.useLines { lines ->
            lines.joinToString("\n") { line ->
              when {
                line.startsWith("sdk.dir=") -> "sdk.dir=$androidSdkDirPath"
                else                        -> line
              }
            }.suffixIfNot("\n")
          }
        )
      }
    }
  }
}