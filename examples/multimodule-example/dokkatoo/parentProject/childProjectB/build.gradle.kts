plugins {
  kotlin("jvm")
  id("dev.adamko.dokkatoo") version "1.0.1-SNAPSHOT"
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("ModuleB.md")
  }
  modulePath.set("childProjectB") // match the original dokka default
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask>().configureEach {
  dokkaSourceSets.configureEach {
    sourceSetScope.set(":parentProject:childProjectB:dokkaHtmlPartial")
  }
}
