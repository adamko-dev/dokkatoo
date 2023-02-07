package dev.adamko.dokkatoo.utils

/** Replace all newlines with `\n`, so the String can be used in assertions cross-platform */
fun String.invariantNewlines(): String =
  lines().joinToString("\n")
