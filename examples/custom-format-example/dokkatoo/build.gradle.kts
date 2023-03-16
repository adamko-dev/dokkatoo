import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters

plugins {
  kotlin("jvm") version "1.7.20"
  id("dev.adamko.dokkatoo") version "0.0.5-SNAPSHOT"
}

dokkatoo {
  moduleName.set("customFormat-example")
  pluginsConfiguration.named<DokkaHtmlPluginParameters>("html") {
    customStyleSheets.from("logo-styles.css")
    customAssets.from("ktor-logo.png")
    footerMessage.set("(c) Custom Format Dokka example")
  }
}
