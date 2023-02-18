plugins {
  kotlin("jvm")
  id("dev.adamko.dokkatoo") version "0.0.2-SNAPSHOT"
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("Module.md")
  }
  modulePath.set("childProjectB") // match the original dokka default
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask>().configureEach {
  dokkaSourceSets.configureEach {
    sourceSetScope.set(":parentProject:childProjectB:dokkaHtmlPartial")
  }
}
