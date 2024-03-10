package buildsrc.conventions

import buildsrc.utils.consumable
import buildsrc.utils.declarable
import buildsrc.utils.resolvable
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE

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
}

val exampleProjectsResolvable: Configuration by configurations.creating {
  resolvable()
  extendsFrom(exampleProjects)
  attributes {
    attribute(exampleProjectsAttribute, "dokka")
    attribute(CATEGORY_ATTRIBUTE, objects.named("dokkatoo-example-projects"))
  }
}

val exampleProjectsConsumable: Configuration by configurations.creating {
  consumable()
  extendsFrom(exampleProjects)
  attributes {
    attribute(exampleProjectsAttribute, "dokka")
    attribute(CATEGORY_ATTRIBUTE, objects.named("dokkatoo-example-projects"))
  }
}
