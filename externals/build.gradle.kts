import buildsrc.conventions.utils.*

plugins {
  buildsrc.conventions.base
}

dependencies {
  kotlinDokkaSource("kotlin:dokka:1.7.20@zip")
}

val kotlinDokkaPrepareSource by tasks.registering(Sync::class) {
  group = "externals"
  description = "Download & unpack Kotlin Dokka source code"
  from(
    @Suppress("UnstableApiUsage")
    configurations.kotlinDokkaSource.flatMap { src ->
      src.incoming
        .artifactView { lenient(true) }
        .artifacts
        .resolvedArtifacts
        .map { artifacts -> artifacts.map { zipTree(it.file) } }
    }
  ) {
    // drop the first dir (dokka-$version)
    eachFile {
      relativePath = relativePath.dropDirectories(1)
    }
  }
  into(layout.projectDirectory.dir("kotlin-dokka"))
  exclude(
    "*.github",
    "*.gradle",
    "**/gradlew",
    "**/gradlew.bat",
    "**/gradle/wrapper/gradle-wrapper.jar",
    "**/gradle/wrapper/gradle-wrapper.properties",
  )
}

configurations.kotlinDokkaSourceElements.configure {
  outgoing {
    artifact(kotlinDokkaPrepareSource.map { it.destinationDir })
  }
}
