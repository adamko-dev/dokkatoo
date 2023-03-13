import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
  `kotlin-dsl`
}

dependencies {
  implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")
  implementation(libs.gradlePlugin.bcvMu)
  implementation(libs.gradlePlugin.dokkatoo)
  implementation(libs.gradlePlugin.gradlePublishPlugin)
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}
