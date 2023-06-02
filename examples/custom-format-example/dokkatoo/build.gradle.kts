plugins {
  kotlin("jvm") version "1.8.10"
  id("dev.adamko.dokkatoo") version "1.5.0-SNAPSHOT"
}

dokkatoo {
  moduleName.set("customFormat-example")
  pluginsConfiguration.html {
    // Custom format adds a custom logo
    customStyleSheets.from("logo-styles.css")
    customAssets.from("ktor-logo.png")
    footerMessage.set("(c) Custom Format Dokka example")
  }
}
