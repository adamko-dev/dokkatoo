package dev.adamko.dokkatoo.internal

@DokkatooInternalApi
abstract class HasFormatName {
  abstract val formatName: String?

  /** Appends [formatName] to the end of the string, camelcase style, if [formatName] is not null */
  protected fun String.appendFormat(): String =
    when (val name = formatName) {
      null -> this
      else -> this + name.uppercaseFirstChar()
    }
}
