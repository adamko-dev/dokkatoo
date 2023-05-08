package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.uppercaseFirstChar


@DokkatooInternalApi
interface HasDokkatooFormatName {
  val formatName: String?

  /** Appends [formatName] to the end of the string, camelcase style, if [formatName] is not null */
  fun String.appendFormat(): String =
    when (val name = formatName) {
      null -> this
      else -> this + name.uppercaseFirstChar()
    }
}
