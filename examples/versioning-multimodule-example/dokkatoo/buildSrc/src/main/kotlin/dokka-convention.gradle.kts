/**
 * Common conventions for generating documentation with Dokkatoo.
 */

plugins {
  id("dev.adamko.dokkatoo")
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    sourceLink {
      // Read docs for more details: https://kotlinlang.org/docs/dokka-gradle.html#source-link-configuration
      remoteUrl("https://github.com/Kotlin/dokka/tree/master/examples/gradle/versioning-multimodule-example")
      localDirectory.set(rootDir)
    }
  }
}
