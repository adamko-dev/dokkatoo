plugins {
  `java-mongodb-library-convention`
  `dokka-convention`
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("Module.md")
  }

  dokkatooSourceSets.javaMain {
    displayName.set("Java")
  }

  // non-main source sets are suppressed by default
  dokkatooSourceSets.javaMongodbSupport {
    suppress.set(false)
    displayName.set("MongoDB")
  }
}
