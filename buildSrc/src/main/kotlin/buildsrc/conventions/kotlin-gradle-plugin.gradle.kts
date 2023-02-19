package buildsrc.conventions

plugins {
  id("buildsrc.conventions.base")
  id("org.gradle.kotlin.kotlin-dsl")
  id("com.gradle.plugin-publish")
}

tasks.validatePlugins {
  enableStricterValidation.set(true)
}

java {
  withSourcesJar()
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}
