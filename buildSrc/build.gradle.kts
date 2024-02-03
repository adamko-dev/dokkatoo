import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
  `kotlin-dsl`
}

dependencies {
  implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")
  implementation(libs.gradlePlugin.bcvMu)
  implementation(libs.gradlePlugin.dokkatoo)
  implementation(libs.gradlePlugin.gradlePublishPlugin)
  implementation("org.jetbrains.kotlin:kotlin-serialization:$embeddedKotlinVersion")

  implementation("org.tomlj:tomlj:1.1.1") {
    because("parse Dokka's libs.version.toml, so Dokkatoo can use the same versions")
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}
