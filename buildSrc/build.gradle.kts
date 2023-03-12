import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
  `kotlin-dsl`
}

dependencies {
  implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")
  implementation("com.gradle.publish:plugin-publish-plugin:1.1.0")
  implementation("dev.adamko.kotlin.binary_compatibility_validator:bcv-gradle-plugin:0.0.4-SNAPSHOT")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}
