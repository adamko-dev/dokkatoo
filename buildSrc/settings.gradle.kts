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

    //@formatter:off
    val spaceUsername = decode("NTI0MTQ3YTctNmI3OS00MTIyLWJmNTItOWY3MzUzZWE2Mjg1")
    val spacePassword = decode("ZXlKaGJHY2lPaUpTVXpVeE1pSjkuZXlKemRXSWlPaUkxTWpReE5EZGhOeTAyWWpjNUxUUXhNakl0WW1ZMU1pMDVaamN6TlRObFlUWXlPRFVpTENKaGRXUWlPaUkxTWpReE5EZGhOeTAyWWpjNUxUUXhNakl0WW1ZMU1pMDVaamN6TlRObFlUWXlPRFVpTENKdmNtZEViMjFoYVc0aU9pSmhaR0Z0YTI5a1pYWWlMQ0p1WVcxbElqb2liV0YyWlc0dFkyOXVjM1Z0WlhJaUxDSnBjM01pT2lKb2RIUndjenBjTDF3dllXUmhiV3R2WkdWMkxtcGxkR0p5WVdsdWN5NXpjR0ZqWlNJc0luQmxjbTFmZEc5clpXNGlPaUpPTTBsVFdqRmpUMnRvVmlJc0luQnlhVzVqYVhCaGJGOTBlWEJsSWpvaVUwVlNWa2xEUlNJc0ltbGhkQ0k2TVRjd05UWTRNRFUwTVgwLk1fUENVb1JiaVRoamZzZzQ2NEhTdjBzaUlPUmJpc0xwN3otQV92OXNnVGdjNXliUm8xakMzeW80aFptaERiV3FIb3Q4V0lfeG03Uzk4cjlQQmRVVEROTVFlNUZKeVJQc0J0T1VMZTl1bnFpdFVJa1RMTHQzdUpzQW0xT1E2RnRaUXVTbTlySzhVY2NWNUFZNDl6ZXE5S2J1OGlEb251UmNWb1RQSlFFUkdVYw==")
    //@formatter:on

    maven("https://maven.pkg.jetbrains.space/adamkodev/p/main/maven-releases") {
      name = "JetBrainsSpaceReleases"
      credentials {
        username = spaceUsername.decodeToString()
        password = spacePassword.decodeToString()
      }
      mavenContent { releasesOnly() }
    }

    maven("https://maven.pkg.jetbrains.space/adamkodev/p/main/maven-snapshots") {
      name = "JetBrainsSpaceSnapshots"
      credentials {
        username = spaceUsername.decodeToString()
        password = spacePassword.decodeToString()
      }
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
