plugins {
  kotlin("multiplatform") version "1.9.0"
  id("dev.adamko.dokkatoo") version "2.0.0"
}

group = "org.dokka.example"
version = "1.0-SNAPSHOT"

kotlin {
  jvm() // Creates a JVM target with the default name "jvm"
  linuxX64("linux")
  macosX64("macos")
  js(IR) {
    browser()
  }
  sourceSets {
    commonMain {
      dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
      }
    }
  }
}

dokkatoo {
  // Create a custom source set not known to the Kotlin Gradle Plugin
  dokkatooSourceSets.register("customSourceSet") {
    jdkVersion = 9
    displayName = "custom"
    sourceRoots.from("src/customJdk9/kotlin")
  }
}


//region DON'T COPY - this is only needed for internal Dokkatoo integration tests
dokkatoo {
  sourceSetScopeDefault = ":dokkaHtml"
}
//endregion
