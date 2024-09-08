package dev.adamko.dokkatoo.utils

import io.kotest.core.config.AbstractProjectConfig

@Suppress("unused") // Kotest loads this class via system property `kotest.framework.config.fqn`
object KotestProjectConfig : AbstractProjectConfig() {
  init {
    displayFullTestPath = true
  }
}
