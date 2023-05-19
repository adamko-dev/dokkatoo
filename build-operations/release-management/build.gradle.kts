plugins {
  base
}

tasks.register("run") {
  val dokkatoo = gradle.includedBuild("dokkatoo-components")
  dependsOn(
    dokkatoo.task(":check"),
    dokkatoo.task(":docs:check"),
    dokkatoo.task(":dokkatoo-plugin:check"),
    dokkatoo.task(":dokkatoo-plugin-integration-tests:check"),
    dokkatoo.task(":examples:check"),
  )
}
