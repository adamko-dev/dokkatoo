plugins {
  kotlin("jvm") version "1.7.20" apply false
  id("dev.adamko.dokkatoo") version "1.0.1-SNAPSHOT"
}

dependencies {
  dokkatoo(project(":parentProject:childProjectA"))
  dokkatoo(project(":parentProject:childProjectB"))
  dokkatooPluginHtml("org.jetbrains.dokka:all-modules-page-plugin:1.7.20")
  dokkatooPluginHtml("org.jetbrains.dokka:templating-plugin:1.7.20")
}

dokkatoo {
  moduleName.set("Dokka MultiModule Example")
}
