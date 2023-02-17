plugins {
  buildsrc.conventions.`maven-publish-test`
  buildsrc.conventions.`dokkatoo-example-projects`
}


dependencies {
//  testMavenPublication(projects.modules.dokkatooPlugin)
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
      builtBy(tasks.setupDokkaTemplateProjects, tasks.updateGradlePropertiesInDokkatooExamples)
      type = "directory"
    }
  }
//  outgoing {
//    listOf(
//      "custom-format-example",
//      "gradle-example",
//      "kotlin-as-java-example",
//      "library-publishing-example",
//      "multimodule-example",
//      "multiplatform-example",
//      "versioning-multimodule-example",
//    ).forEach { exampleDir ->
//      artifact(layout.projectDirectory.dir("$exampleDir/dokka")) {
//        builtBy(tasks.setupDokkaTemplateProjects)
//      }
//      artifact(layout.projectDirectory.dir("$exampleDir/dokkatoo")) {
//        builtBy(tasks.setupDokkaTemplateProjects, tasks.updateGradlePropertiesInDokkatooExamples)
//      }
//    }
//  }
}
