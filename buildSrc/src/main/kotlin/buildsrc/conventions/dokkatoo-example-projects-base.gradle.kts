package buildsrc.conventions

import buildsrc.utils.consumable
import buildsrc.utils.declarable
import buildsrc.utils.resolvable

plugins {
  id("buildsrc.conventions.base")
}


val exampleProjectsAttribute: Attribute<String> =
  Attribute.of("example-projects", String::class.java)

dependencies.attributesSchema {
  attribute(exampleProjectsAttribute)
}


val exampleProjects: Configuration by configurations.creating {
  declarable()
  attributes { attribute(exampleProjectsAttribute, "dokka") }
}

val exampleProjectsResolvable: Configuration by configurations.creating {
  resolvable()
  extendsFrom(exampleProjects)
  attributes { attribute(exampleProjectsAttribute, "dokka") }
}

val exampleProjectsConsumable: Configuration by configurations.creating {
  consumable()
  extendsFrom(exampleProjects)
  attributes { attribute(exampleProjectsAttribute, "dokka") }
}
