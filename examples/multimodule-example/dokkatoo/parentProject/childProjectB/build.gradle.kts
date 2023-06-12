plugins {
  kotlin("jvm")
  id("dev.adamko.dokkatoo") version "1.5.0-SNAPSHOT"
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("ModuleB.md")
  }
  modulePath.set("childProjectB") // match the original dokka default
}

//region DON'T COPY - this is only needed for internal Dokkatoo integration tests
tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  generator.dokkaSourceSets.configureEach {
    sourceSetScope.set(":parentProject:childProjectB:dokkaHtmlPartial")
  }
}
//endregion
