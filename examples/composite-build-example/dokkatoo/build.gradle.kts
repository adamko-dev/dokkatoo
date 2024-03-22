plugins {
  base
}

tasks.build {
  dependsOn(gradle.includedBuild("docs").task(":dokkatooGenerate"))
}

group = "foo.example.composite.builds"
version = "1.0.1"


tasks.clean {
  dependsOn(
    gradle.includedBuild("docs").task(":clean"),
    gradle.includedBuild("module-kakapo").task(":clean"),
    gradle.includedBuild("module-kea").task(":clean"),
  )
}
