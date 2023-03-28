package dev.adamko.dokkatoo.internal

internal fun <T, R> Set<T>.mapToSet(transform: (T) -> R): Set<R> =
  mapTo(mutableSetOf(), transform)
