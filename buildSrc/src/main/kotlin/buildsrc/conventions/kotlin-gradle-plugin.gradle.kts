package buildsrc.conventions

plugins {
  id("buildsrc.conventions.base")
  id("buildsrc.conventions.java-base")
  id("org.gradle.kotlin.kotlin-dsl")
  id("com.gradle.plugin-publish")
}

tasks.validatePlugins {
  enableStricterValidation.set(true)
}

sourceSets {
  configureEach {
    java.setSrcDirs(emptyList<File>())
  }
}
