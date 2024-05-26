plugins {
  `java-library-convention`
  `dokka-convention`
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("Module.md")
  }
}
