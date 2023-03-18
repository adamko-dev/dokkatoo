plugins {
  buildsrc.conventions.`maven-publish-test`
  buildsrc.conventions.`dokkatoo-example-projects`
}

tasks.setupDokkaTemplateProjects {
  destinationToSources.set(
    mapOf(
      //@formatter:off
      "custom-format-example/dokka"          to listOf("examples/gradle/dokka-customFormat-example"),
      "gradle-example/dokka"                 to listOf("examples/gradle/dokka-gradle-example"),
      "kotlin-as-java-example/dokka"         to listOf("examples/gradle/dokka-kotlinAsJava-example"),
      "library-publishing-example/dokka"     to listOf("examples/gradle/dokka-library-publishing-example"),
      "multimodule-example/dokka"            to listOf("examples/gradle/dokka-multimodule-example"),
      "multiplatform-example/dokka"          to listOf("examples/gradle/dokka-multiplatform-example"),
      "versioning-multimodule-example/dokka" to listOf("examples/gradle/dokka-versioning-multimodule-example"),
      //@formatter:on
    ).mapKeys { (dest, _) -> projectDir.resolve(dest) }
  )
}

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
