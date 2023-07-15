plugins {
  id("buildsrc.conventions.maven-publish-test")
  id("buildsrc.conventions.dokkatoo-example-projects")
}

group = "dev.adamko.dokkatoo.examples"
version = "2.0.0-SNAPSHOT"

dokkaTemplateProjects {
  register(
    source = "examples/gradle/dokka-customFormat-example",
    destination = "custom-format-example-dokka"
  )
  register(
    source = "examples/gradle/dokka-gradle-example",
    destination = "gradle-example-dokka"
  )
  register(
    source = "examples/gradle/dokka-kotlinAsJava-example",
    destination = "kotlin-as-java-example-dokka"
  )
  register(
    source = "examples/gradle/dokka-library-publishing-example",
    destination = "library-publishing-example-dokka"
  )
  register(
    source = "examples/gradle/dokka-multimodule-example",
    destination = "multimodule-example-dokka"
  )
  register(
    source = "examples/gradle/dokka-multiplatform-example",
    destination = "multiplatform-example-dokka"
  )
  register(
    source = "examples/gradle/dokka-versioning-multimodule-example",
    destination = "versioning-multimodule-example-dokka"
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
