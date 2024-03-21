package buildsrc.conventions

import buildsrc.settings.DokkaSourceDownloaderSettings
import buildsrc.utils.consumable
import buildsrc.utils.declarable
import buildsrc.utils.dropDirectories
import buildsrc.utils.resolvable
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
  id("buildsrc.conventions.base")
}

val dsdExt: DokkaSourceDownloaderSettings = extensions.create<DokkaSourceDownloaderSettings>(
  DokkaSourceDownloaderSettings.EXTENSION_NAME
)

val kotlinDokkaSource: Configuration by configurations.creating {
  description = "Source code for https://github.com/Kotlin/dokka/"
  declarable()
}

val kotlinDokkaSourceResolvable: Configuration by configurations.creating {
  resolvable()
  extendsFrom(kotlinDokkaSource)
  attributes {
    attribute(USAGE_ATTRIBUTE, objects.named("externals-dokka-src"))
    attribute(CATEGORY_ATTRIBUTE, objects.named("externals-dokka-src"))
  }
}

val kotlinDokkaSourceConsumable: Configuration by configurations.creating {
  consumable()
  extendsFrom(kotlinDokkaSource)
  attributes {
    attribute(USAGE_ATTRIBUTE, objects.named("externals-dokka-src"))
    attribute(CATEGORY_ATTRIBUTE, objects.named("externals-dokka-src"))
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
    kotlinDokkaSourceResolvable.incoming
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
