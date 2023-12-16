plugins {
  kotlin("jvm") apply false
  `dokka-convention`
}

dependencies {
  dokkatoo(project(":parentProject:childProjectA"))
  dokkatoo(project(":parentProject:childProjectB"))

  dokkatooPluginHtml("org.jetbrains.dokka:all-modules-page-plugin")
  dokkatooPluginHtml("org.jetbrains.dokka:templating-plugin")
}

dokkatoo {
  moduleName.set("Dokka MultiModule Example")
}
