plugins {
  kotlin("jvm") version "1.8.22" apply false
  id("dev.adamko.dokkatoo") version "1.5.0-SNAPSHOT"
}

dependencies {
  dokkatoo(project(":parentProject:childProjectA"))
  dokkatoo(project(":parentProject:childProjectB"))
  dokkatooPluginHtml(
    dokkatoo.versions.jetbrainsDokka.map { dokkaVersion ->
      "org.jetbrains.dokka:all-modules-page-plugin:$dokkaVersion"
    }
  )
  dokkatooPluginHtml(
    dokkatoo.versions.jetbrainsDokka.map { dokkaVersion ->
      "org.jetbrains.dokka:templating-plugin:$dokkaVersion"
    }
  )
}

dokkatoo {
  moduleName.set("Dokka MultiModule Example")
}
