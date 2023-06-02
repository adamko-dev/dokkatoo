plugins {
  kotlin("multiplatform") version "1.8.10"
  id("dev.adamko.dokkatoo") version "1.4.0"
}

group = "org.dokka.example"
version = "1.0-SNAPSHOT"

kotlin {
  jvm() // Creates a JVM target with the default name "jvm"
  linuxX64("linux")
  macosX64("macos")
  js(BOTH) {
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
    jdkVersion.set(9)
    displayName.set("custom")
    sourceRoots.from("src/customJdk9/kotlin")
  }
}



dokkatoo {
  // DON'T COPY - this is only needed for internal Dokkatoo integration tests
  sourceSetScopeDefault.set( /* DON'T COPY */ ":dokkaHtml")
  versions.jetbrainsDokka.set( /* DON'T COPY */ "1.7.20")
}
