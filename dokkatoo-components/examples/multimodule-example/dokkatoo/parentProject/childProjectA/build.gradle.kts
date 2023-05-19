plugins {
  kotlin("jvm")
  id("dev.adamko.dokkatoo") version "1.4.0-SNAPSHOT"
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("ModuleA.md")
  }
  modulePath.set("childProjectA") // match the original dokka default
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  generator.dokkaSourceSets.configureEach {
    sourceSetScope.set(":parentProject:childProjectA:dokkaHtmlPartial")
  }
}
