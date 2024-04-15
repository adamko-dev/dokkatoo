import buildsrc.utils.excludeProjectConfigurationDirs
import buildsrc.utils.initIdeProjectLogo
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP

plugins {
  buildsrc.conventions.base
  idea
}

group = "dev.adamko.dokkatoo"
version = "2.3.1"

excludeProjectConfigurationDirs(idea)

tasks.prepareKotlinBuildScriptModel {
  initIdeProjectLogo("documentation/media/kayray-logo.svg")
}

val dokkatooVersion by tasks.registering {
  description = "prints the Dokkatoo project version (used during release to verify the version)"
  group = "help"
  val version = providers.provider { project.version.toString() }
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
