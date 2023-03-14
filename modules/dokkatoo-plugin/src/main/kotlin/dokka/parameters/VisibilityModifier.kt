package dev.adamko.dokkatoo.dokka.parameters

import org.jetbrains.dokka.DokkaConfiguration

enum class VisibilityModifier(
  internal val dokkaType: DokkaConfiguration.Visibility
) {
  /** `public` modifier for Java, default visibility for Kotlin */
  PUBLIC(DokkaConfiguration.Visibility.PUBLIC),
  /** `private` modifier for both Kotlin and Java */
  PRIVATE(DokkaConfiguration.Visibility.PRIVATE),
  /** `protected` modifier for both Kotlin and Java */
  PROTECTED(DokkaConfiguration.Visibility.PROTECTED),
  /** Kotlin-specific `internal` modifier */
  INTERNAL(DokkaConfiguration.Visibility.INTERNAL),
  /** Java-specific package-private visibility (no modifier) */
  PACKAGE(DokkaConfiguration.Visibility.PACKAGE),
  ;

  companion object {
    internal fun Set<VisibilityModifier>.convertToDokkaType() =
      mapTo(mutableSetOf(), VisibilityModifier::dokkaType)
  }
}
