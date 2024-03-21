import buildsrc.utils.excludeGeneratedGradleDsl
import buildsrc.utils.initIdeProjectLogo
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP

plugins {
  buildsrc.conventions.base
  idea
}

group = "dev.adamko.dokkatoo"
version = "2.3.0-SNAPSHOT"


idea {
  module {
    excludeGeneratedGradleDsl(layout)

    excludeDirs.apply {
      // exclude .gradle, IDE dirs from nested projects (e.g. example & template projects)
      // so IntelliJ project-wide search isn't cluttered with irrelevant files
      val excludedDirs = setOf(
        ".idea",
        ".gradle",
        "build",
        "gradle/wrapper",
        "ANDROID_SDK",
        "examples/versioning-multimodule-example/dokkatoo/previousDocVersions",
        "examples/versioning-multimodule-example/dokka/previousDocVersions",
      )
      addAll(
        projectDir.walk().filter { file ->
          excludedDirs.any {
            file.invariantSeparatorsPath.endsWith("/$it")
          }
        }
      )
    }
  }
}

initIdeProjectLogo("modules/docs/images/logo-icon.svg")

val dokkatooVersion by tasks.registering {
  description = "prints the Dokkatoo project version (used during release to verify the version)"
  group = "help"
  val version = providers.provider { project.version }
  doLast {
    logger.quiet("${version.orNull}")
  }
}


val verifyVersionCatalogKotlinVersion by tasks.registering {
  description = "Verify the Version Catalog Kotlin version matches Gradle's embedded Kotlin version"
  //  https://docs.gradle.org/current/userguide/compatibility.html#kotlin
  group = VERIFICATION_GROUP

  val kotlinVersion = libs.versions.kotlin.asProvider()
  inputs.property("kotlinVersion", kotlinVersion)
  val embeddedKotlinVersion = embeddedKotlinVersion
  inputs.property("embeddedKotlinVersion", embeddedKotlinVersion)

  doLast {
    require(kotlinVersion.get() == embeddedKotlinVersion) {
      "Version Catalog Kotlin version (${kotlinVersion.get()}) did not match embeddedKotlinVersion ($embeddedKotlinVersion)"
    }
  }
}

tasks.check {
  dependsOn(verifyVersionCatalogKotlinVersion)
}
