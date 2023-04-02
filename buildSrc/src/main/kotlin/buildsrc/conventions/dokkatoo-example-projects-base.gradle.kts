package buildsrc.conventions

import buildsrc.conventions.Maven_publish_test_gradle.MavenPublishTest
import buildsrc.utils.asConsumer
import buildsrc.utils.asProvider
import buildsrc.tasks.SetupDokkaProjects

plugins {
  id("buildsrc.conventions.base")
}


val exampleProjectsAttribute = Attribute.of("example-projects", String::class.java)
dependencies.attributesSchema {
  attribute(exampleProjectsAttribute)
}


val exampleProjects by configurations.registering {
  asConsumer()
  isVisible = false
  attributes { attribute(exampleProjectsAttribute, "dokka") }
}

val exampleProjectsElements by configurations.registering {
  asProvider()
  isVisible = true
  attributes { attribute(exampleProjectsAttribute, "dokka") }
}
