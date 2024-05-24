plugins {
  `java-application-convention`
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

application {
  mainClass = "demo.MyJavaApplication"
}
