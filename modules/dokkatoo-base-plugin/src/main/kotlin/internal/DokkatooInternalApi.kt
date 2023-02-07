package dev.adamko.dokkatoo.internal

import kotlin.RequiresOptIn.Level

@RequiresOptIn(
  "Internal API - may change at any time without notice",
  level = Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class DokkatooInternalApi
