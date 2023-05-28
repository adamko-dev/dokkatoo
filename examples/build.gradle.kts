plugins {
  buildsrc.conventions.`maven-publish-test`
  buildsrc.conventions.`dokkatoo-example-projects`
}

dokkaTemplateProjects {
  register(
    source = "examples/gradle/dokka-customFormat-example",
    destination = "custom-format-example/dokka"
  )
  register(
    source = "examples/gradle/dokka-gradle-example",
    destination = "gradle-example/dokka"
  )
  register(
    source = "examples/gradle/dokka-kotlinAsJava-example",
    destination = "kotlin-as-java-example/dokka"
  )
  register(
    source = "examples/gradle/dokka-library-publishing-example",
    destination = "library-publishing-example/dokka"
  )
  register(
    source = "examples/gradle/dokka-multimodule-example",
    destination = "multimodule-example/dokka"
  )
  register(
    source = "examples/gradle/dokka-multiplatform-example",
    destination = "multiplatform-example/dokka"
  )
  register(
    source = "examples/gradle/dokka-versioning-multimodule-example",
    destination = "versioning-multimodule-example/dokka"
  )
}

//tasks.setupDokkaTemplateProjects {
//  destinationToSources.set(
//    mapOf(
//      //@formatter:off
//      "custom-format-example/dokka"          to listOf("examples/gradle/dokka-customFormat-example"),
//      "gradle-example/dokka"                 to listOf("examples/gradle/dokka-gradle-example"),
//      "kotlin-as-java-example/dokka"         to listOf("examples/gradle/dokka-kotlinAsJava-example"),
//      "library-publishing-example/dokka"     to listOf("examples/gradle/dokka-library-publishing-example"),
//      "multimodule-example/dokka"            to listOf("examples/gradle/dokka-multimodule-example"),
//      "multiplatform-example/dokka"          to listOf("examples/gradle/dokka-multiplatform-example"),
//      "versioning-multimodule-example/dokka" to listOf("examples/gradle/dokka-versioning-multimodule-example"),
//      //@formatter:on
//    ).mapKeys { (dest, _) -> projectDir.resolve(dest) }
//  )
//}

configurations.exampleProjectsElements.configure {
  outgoing {
    artifact(projectDir) {
      builtBy(tasks.updateDokkatooExamples)
      type = "directory"
    }
  }
}

dokkaSourceDownload {
  dokkaVersion.set(libs.versions.kotlin.dokka)
}
