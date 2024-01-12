plugins {
  kotlin("jvm")
  `dokka-convention`
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("ModuleB.md")
  }
}

//region DON'T COPY - this is only needed for internal Dokkatoo integration tests
dokkatoo {
  modulePath = "childProjectB" // match the original dokka default
}
tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  generator.dokkaSourceSets.configureEach {
    sourceSetScope = ":parentProject:childProjectB:dokkaHtmlPartial"
  }
}
//endregion
