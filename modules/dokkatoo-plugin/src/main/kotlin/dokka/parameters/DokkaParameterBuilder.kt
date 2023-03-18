package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkatooInternalApi

/**
 * Implementing classes should build a Dokka Parameters type
 * ([DokkaParametersKxs], for example).
 *
 * It is equivalent to [org.jetbrains.dokka.DokkaConfigurationBuilder].
 *
 * (This interface is just used for alignment and is not strictly necessary.)
 */
@DokkatooInternalApi
internal interface DokkaParameterBuilder<T> {
  @DokkatooInternalApi
  fun build(): T
}
