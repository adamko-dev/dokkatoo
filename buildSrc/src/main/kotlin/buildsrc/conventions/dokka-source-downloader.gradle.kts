package buildsrc.conventions

import buildsrc.conventions.utils.asConsumer
import buildsrc.conventions.utils.asProvider
import buildsrc.conventions.utils.dropDirectories

plugins {
  id("buildsrc.conventions.base")
}

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
  kotlinDokkaSource("kotlin:dokka:1.7.20@zip")
}

val prepareDokkaSource by tasks.registering(Sync::class) {
  group = "dokka setup"
  description = "Download & unpack Kotlin Dokka source code"
  from(
    kotlinDokkaSource.incoming
      .artifactView { lenient(true) }
      .artifacts
      .resolvedArtifacts.map { artifacts ->
        artifacts.map { zipTree(it.file) }
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
