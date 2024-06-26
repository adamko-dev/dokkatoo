package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier.Companion.dokkaType
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.shouldBe
import org.jetbrains.dokka.DokkaConfiguration

class VisibilityModifierTest : FunSpec({

  test("DokkaConfiguration.Visibility should have equivalent VisibilityModifier") {
    DokkaConfiguration.Visibility.values().shouldForAll { dokkaVisibility ->
      VisibilityModifier.values.map { it.dokkaType }.shouldForOne { it shouldBe dokkaVisibility }
    }
  }
})
