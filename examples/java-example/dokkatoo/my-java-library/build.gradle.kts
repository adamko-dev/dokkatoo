plugins {
  `java-library`
  `dokka-convention`
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("Module.md")
  }
}
