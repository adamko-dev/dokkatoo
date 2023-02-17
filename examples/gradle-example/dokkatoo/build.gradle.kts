plugins {
  kotlin("jvm") version "1.7.20"
  id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
}

dokkatoo {
  moduleName.set("Dokka Gradle Example")
  dokkatooSourceSets.named("main") {
    includes.from("Module.md")
    sourceLink {
      localDirectory.set(file("src/main/kotlin"))
      remoteUrl("https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-gradle-example/src/main/kotlin")
//      remoteUrl("https://github.com/adamko-dev/dokkatoo/tree/main/examples/gradle-example/dokkatoo/src/main/kotlin")
      remoteLineSuffix.set("#L")
    }
  }
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask>().configureEach {
  dokkaSourceSets.configureEach {
    sourceSetScope.set(":dokkaHtml") // only necessary for testing
  }
}
