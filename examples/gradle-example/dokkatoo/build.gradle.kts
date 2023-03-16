plugins {
  kotlin("jvm") version "1.8.10"
  id("dev.adamko.dokkatoo") version "1.0.1-SNAPSHOT"
}

dokkatoo {
  // used as project name in the header
  moduleName.set("Dokka Gradle Example")

  dokkatooSourceSets.named("main") {

    // contains descriptions for the module and the packages
    includes.from("Module.md")

    // adds source links that lead to this repository, allowing readers
    // to easily find source code for inspected declarations
    sourceLink {
      localDirectory.set(file("src/main/kotlin"))
      remoteUrl("https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-gradle-example/src/main/kotlin")
      //remoteUrl("https://github.com/adamko-dev/dokkatoo/tree/main/examples/gradle-example/dokkatoo/src/main/kotlin")
      remoteLineSuffix.set("#L")
    }
  }
}
