plugins {
  kotlin("jvm")
  id("dev.adamko.dokkatoo") version "1.4.0"
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("ModuleB.md")
  }
  modulePath.set("childProjectB") // match the original dokka default
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  generator.dokkaSourceSets.configureEach {
    sourceSetScope.set(":parentProject:childProjectB:dokkaHtmlPartial")
  }
}
