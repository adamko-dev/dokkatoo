plugins {
  kotlin("jvm")
  id("dev.adamko.dokkatoo") version "1.3.0"
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("ModuleA.md")
  }
  modulePath.set("childProjectA") // match the original dokka default
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask>().configureEach {
  dokkaSourceSets.configureEach {
    sourceSetScope.set(":parentProject:childProjectA:dokkaHtmlPartial")
  }
}
