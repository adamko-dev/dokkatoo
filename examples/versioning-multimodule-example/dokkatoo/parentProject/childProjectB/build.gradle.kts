plugins {
  kotlin("jvm")
  `dokka-convention`
}

//region DON'T COPY - this is only needed for internal Dokkatoo integration tests
dokkatoo {
  modulePath.set("childProjectB") // match the original dokka default
}
tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  generator.dokkaSourceSets.configureEach {
    sourceSetScope.set(":parentProject:childProjectB:dokkaHtmlPartial")
  }
}
//endregion
