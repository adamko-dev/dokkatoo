plugins {
  kotlin("jvm") apply false
  `dokka-convention`
}

dependencies {
  dokkatoo(project(":parentProject:childProjectA"))
  dokkatoo(project(":parentProject:childProjectB"))
}

dokkatoo {
  moduleName.set("Dokka MultiModule Example")
}
