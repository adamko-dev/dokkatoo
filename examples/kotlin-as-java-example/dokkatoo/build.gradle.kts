plugins {
  kotlin("jvm") version "1.9.23"
  id("dev.adamko.dokkatoo") version "2.4.0"
}

dependencies {
  testImplementation(kotlin("test-junit"))

  // Will apply the plugin only to the `:dokkaHtml` task
  // (Dokkatoo will automatically add the version)
  dokkatooPlugin("org.jetbrains.dokka:kotlin-as-java-plugin")
}
