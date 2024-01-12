plugins {
  kotlin("jvm")
  `dokka-convention`
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("ModuleA.md")
  }
}

//region DON'T COPY - this is only needed for internal Dokkatoo integration tests
dokkatoo {
  modulePath = "childProjectA" // match the original dokka default
}
tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  generator.dokkaSourceSets.configureEach {
    sourceSetScope = ":parentProject:childProjectA:dokkaHtmlPartial"
  }
}
//endregion
