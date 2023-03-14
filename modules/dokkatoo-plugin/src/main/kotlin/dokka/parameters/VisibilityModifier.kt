package dev.adamko.dokkatoo.dokka.parameters

import org.jetbrains.dokka.DokkaConfiguration

/**
 * Denotes the
 * [visibility modifier](https://kotlinlang.org/docs/visibility-modifiers.html)
 * of a source code elements.
 *
 * @see org.jetbrains.dokka.DokkaConfiguration.Visibility
 */
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

    internal val DEFAULT = PUBLIC

    internal fun Set<VisibilityModifier>.convertToDokkaTypes() =
      mapTo(mutableSetOf(), VisibilityModifier::dokkaType)
  }
}
