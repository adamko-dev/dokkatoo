package dev.adamko.dokkatoo.internal

import java.time.Duration


/**
 * Stable implementation of [kotlin.time.measureTime].
 */
// can't use kotlin.Duration or kotlin.time.measureTime {} because
// the implementation isn't stable across Kotlin versions
internal fun measureTime(block: () -> Unit): Duration =
  measureTimedValue(block).second


/**
 * Stable implementation of [kotlin.time.measureTimedValue].
 */
// can't use kotlin.time.measureTime {} because the implementation isn't stable across Kotlin versions
internal fun <T> measureTimedValue(block: () -> T): Pair<T, Duration> {
  val start = System.nanoTime()
  val value = block()
  val end = System.nanoTime()
  return value to Duration.ofNanos(end - start)
}
