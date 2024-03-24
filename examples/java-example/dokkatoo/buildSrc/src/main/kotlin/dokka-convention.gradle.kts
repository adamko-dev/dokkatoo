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
}

dependencies {
  dokkatooPluginHtml("org.jetbrains.dokka:kotlin-as-java-plugin")
  dokkatooPluginJavadoc("org.jetbrains.dokka:kotlin-as-java-plugin")
  dokkatooPluginJekyll("org.jetbrains.dokka:kotlin-as-java-plugin")
  dokkatooPluginGfm("org.jetbrains.dokka:kotlin-as-java-plugin")
}
