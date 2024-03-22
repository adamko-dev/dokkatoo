/**
 * Common conventions for generating documentation with Dokkatoo.
 */

plugins {
  id("dev.adamko.dokkatoo-html")
}

dokkatoo {
  // Important! Ensure that each project has a distinct module path.
  // See the example README for more information.
  modulePath = rootProject.name + project.path.replace(":", "/")
}
