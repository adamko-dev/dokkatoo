plugins {
  `kotlin-dsl`
  kotlin("plugin.serialization") version embeddedKotlinVersion
}

dependencies {
  implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.7.1"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

  implementation("org.bouncycastle:bcprov-jdk15on:1.69")
  implementation("org.bouncycastle:bcpg-jdk15on:1.69")

  implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.5.1"))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

  implementation(platform("io.ktor:ktor-bom:2.3.1"))
  implementation("io.ktor:ktor-client-auth")
  implementation("io.ktor:ktor-client-core")
  implementation("io.ktor:ktor-client-cio")
  implementation("io.ktor:ktor-client-resources")
  implementation("io.ktor:ktor-client-content-negotiation")
  implementation("io.ktor:ktor-serialization-kotlinx-json")

  implementation("com.squareup.okio:okio:3.3.0")
}
