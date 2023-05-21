plugins {
  id("com.android.library") version "4.0.0"
  kotlin("android") version "1.8.10"
  id("dev.adamko.dokkatoo") version "1.4.0-SNAPSHOT"
}

android {
  defaultConfig {
    minSdkVersion(21)
    setCompileSdkVersion(29)
  }
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.1.0")
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  generator.dokkaSourceSets.configureEach {
    sourceSetScope.set(":dokkaHtml")
  }
}
