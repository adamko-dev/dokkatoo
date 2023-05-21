package buildsrc.utils


/**
 * Title case the first char of a string.
 *
 * (Custom implementation because [toUpperCase]/[uppercase] is deprecated.
 * Can be removed when Gradle is updated)
 */
internal fun String.uppercaseFirstChar(): String = mapFirstChar(Character::toTitleCase)


internal fun String.lowercaseFirstChar(): String = mapFirstChar(Character::toLowerCase)


private inline fun String.mapFirstChar(
  transform: (Char) -> Char
): String = if (isNotEmpty()) transform(this[0]) + substring(1) else this


/**
 * Filters all non-alphanumeric characters and converts the result into camelCase.
 */
internal fun String.toAlphaNumericCamelCase(): String =
  map { if (it.isLetterOrDigit()) it else ' '}
    .joinToString("")
    .split(" ")
    .filter { it.isNotBlank() }
    .joinToString("") { it.uppercaseFirstChar() }
    .lowercaseFirstChar()
