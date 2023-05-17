package internal

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.uppercaseFirstChar

@DokkatooInternalApi
interface HasFormatName {
  val formatName: String?

  /** Appends [formatName] to the end of the string, camelcase style, if [formatName] is not null */
  fun String.appendFormat(): String =
    when (val name = formatName) {
      null -> this
      else -> this + name.uppercaseFirstChar()
    }
}
