plugins {
  `kotlin-dsl`
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion")
  implementation("dev.adamko.dokkatoo:dokkatoo-plugin:2.5.0-SNAPSHOT")
}
