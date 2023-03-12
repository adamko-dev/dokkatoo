plugins {
  kotlin("jvm")
  id("dev.adamko.dokkatoo") version "0.0.3"
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("Module.md")
  }
  modulePath.set("childProjectA") // match the original dokka default
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask>().configureEach {
  dokkaSourceSets.configureEach {
    sourceSetScope.set(":parentProject:childProjectA:dokkaHtmlPartial")
  }
}
