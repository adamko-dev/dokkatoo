plugins {
  id("buildsrc.conventions.base")
  id("buildsrc.conventions.ide")
}

group = "dev.adamko.dokkatoo"
version = "2.0.0-SNAPSHOT"

tasks.check {
  val dokkatooComponents = gradle.includedBuild("dokkatoo-components")
  val dokkatooExamples = gradle.includedBuild("dokkatoo-examples")
  val dokkatooIntegrationTests = gradle.includedBuild("dokkatoo-integration-tests")
  dependsOn(
    dokkatooComponents.task(":check"),
    dokkatooExamples.task(":check"),
    dokkatooIntegrationTests.task(":check"),
  )
}
