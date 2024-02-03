@file:OptIn(ExperimentalEncodingApi::class)

import kotlin.io.encoding.Base64.Default.decode
import kotlin.io.encoding.ExperimentalEncodingApi

rootProject.name = "buildSrc"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
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

    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
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
