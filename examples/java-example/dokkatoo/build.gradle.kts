plugins {
  base
  `dokka-convention`
}

dependencies {
  dokkatoo(project(":my-java-application"))
  dokkatoo(project(":my-java-features"))
  dokkatoo(project(":my-java-library"))

  dokkatooPluginHtml("org.jetbrains.dokka:templating-plugin")
}

dokkatoo {
  moduleName.set("My Java Project")
}
