package dev.adamko.dokkatoo.utils

fun String.splitToPair(delimiter: String) =
  substringBefore(delimiter) to substringAfter(delimiter)
