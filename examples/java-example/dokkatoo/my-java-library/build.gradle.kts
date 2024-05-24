plugins {
  `java-library`
  `dokka-convention`
  id("my-java-conventions")
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("Module.md")
  }
}
