plugins {
  id("com.android.library") version "8.0.2"
  kotlin("android") version "1.9.0"
  id("dev.adamko.dokkatoo") version "2.1.0"
}

android {
  namespace = "org.jetbrains.dokka.it.android"
  defaultConfig {
    minSdkVersion(21)
    setCompileSdkVersion(29)
  }
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.1.0")
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  // Dokka and Dokkatoo fetch Source Set information in different ways. This results in the
  // 'SourceSetId' for each DokkatooSourceSet being slightly different.
  // The SourceSetId is not visible, and just because it's different in Dokka vs Dokkatoo, it
  // doesn't have any impact. But for the purposes of automated testing, they need to be the same.
  // So, forcibly rename the SourceSetId.

  generator.dokkaSourceSets.configureEach {
    sourceSetScope.set(":dokkaHtml")
  }
}
