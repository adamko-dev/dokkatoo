package dev.adamko.dokkatoo.utils

import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.maps.shouldContainExactly

/** @see io.kotest.matchers.maps.shouldContainAll */
fun <K, V> Map<K, V>.shouldContainAll(
  vararg expected: Pair<K, V>
): Unit = shouldContainAll(expected.toMap())

/** @see io.kotest.matchers.maps.shouldContainExactly */
fun <K, V> Map<K, V>.shouldContainExactly(
  vararg expected: Pair<K, V>
): Unit = shouldContainExactly(expected.toMap())
