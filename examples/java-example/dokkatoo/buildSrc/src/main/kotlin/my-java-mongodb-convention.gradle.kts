plugins {
  `java-library`
}

val mongodbSourceSet = sourceSets.create("mongodbSupport") {
  java {
    srcDir("src/mongodb/java")
  }
}

java {
  registerFeature("mongodbSupport") {
    usingSourceSet(mongodbSourceSet)
  }
}
