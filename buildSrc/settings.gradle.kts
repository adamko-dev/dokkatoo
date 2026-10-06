rootProject.name = "buildSrc"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://central.sonatype.com/repository/maven-snapshots/") {
      name = "MavenCentralSnapshots"
      mavenContent { snapshotsOnly() }
    }
  }
}

plugins {
  id("dev.adamko.gradle-kotlin-accessors") version "main-SNAPSHOT"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {

  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

  repositories {
    mavenCentral()
    gradlePluginPortal()

    maven("https://europe-west4-maven.pkg.dev/adamko-dev/adamko-dev-releases") {
      name = "AdamkoDevReleases"
      mavenContent { releasesOnly() }
    }

    maven("https://europe-west4-maven.pkg.dev/adamko-dev/adamko-dev-snapshots") {
      name = "AdamkoDevSnapshots"
      mavenContent { snapshotsOnly() }
    }

    maven("https://central.sonatype.com/repository/maven-snapshots/") {
      name = "MavenCentralSnapshots"
      mavenContent { snapshotsOnly() }
    }
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
