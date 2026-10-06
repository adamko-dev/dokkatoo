package buildsrc.conventions

import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.`java-base`
import org.gradle.kotlin.dsl.withType

plugins {
  id("buildsrc.conventions.base")
  `java`
}

extensions.getByType<JavaPluginExtension>().apply {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
  withSourcesJar()
}

val javaToolchains = extensions.getByType<JavaToolchainService>()

tasks.withType<Test>().configureEach {
  javaLauncher.set(javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(17))
  })
}
