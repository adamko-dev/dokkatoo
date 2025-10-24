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
      remoteUrl("https://github.com/adamko-dev/dokkatoo/tree/main/examples/java-example/dokkatoo")
      localDirectory.set(rootDir)
    }
  }
  pluginsConfiguration.html {
    // By default footerMessage uses the current year.
    // Here we fix the year to 2024 for test stability.
    footerMessage.set("© 2024 Copyright")
  }
}

dependencies {
  dokkatooPlugin("org.jetbrains.dokka:kotlin-as-java-plugin")
}
