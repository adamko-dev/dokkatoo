plugins {
  `java`
  `dokka-convention`
}

dependencies {
  implementation(project(":my-java-library"))
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("Module.md")
  }
}
