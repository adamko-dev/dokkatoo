package buildsrc.conventions

import buildsrc.utils.asConsumer
import buildsrc.utils.asProvider
import buildsrc.utils.dropDirectories
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
  id("buildsrc.conventions.base")
}

interface DokkaSourceDownloaderExtension {
  val dokkaVersion: Property<String>
}

val dsdExt = extensions.create<DokkaSourceDownloaderExtension>("dokkaSourceDownload")

val kotlinDokkaSource by configurations.creating {
  asConsumer()
  attributes {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named("externals-dokka-src"))
  }
}

val kotlinDokkaSourceElements by configurations.registering {
  asProvider()
  attributes {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named("externals-dokka-src"))
  }
}

dependencies {
  kotlinDokkaSource(dsdExt.dokkaVersion.map { "kotlin:dokka:$it@zip" })
}

val prepareDokkaSource by tasks.registering(Sync::class) {
  group = "dokka setup"
  description = "Download & unpack Kotlin Dokka source code"

  inputs.property("dokkaVersion", dsdExt.dokkaVersion).optional(false)

  val archives = serviceOf<ArchiveOperations>()

  from(
    kotlinDokkaSource.incoming
      .artifacts
      .resolvedArtifacts
      .map { artifacts ->
        artifacts.map { archives.zipTree(it.file) }
      }
  ) {
    // drop the first dir (dokka-$version)
    eachFile {
      relativePath = relativePath.dropDirectories(1)
    }
  }

  into(temporaryDir)

  exclude(
    "*.github",
    "*.gradle",
    "**/gradlew",
    "**/gradlew.bat",
    "**/gradle/wrapper/gradle-wrapper.jar",
    "**/gradle/wrapper/gradle-wrapper.properties",
  )
}
