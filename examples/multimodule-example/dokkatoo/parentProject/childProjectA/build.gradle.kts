plugins {
  kotlin("jvm")
  id("dev.adamko.dokkatoo") version "1.1.1-SNAPSHOT"
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
