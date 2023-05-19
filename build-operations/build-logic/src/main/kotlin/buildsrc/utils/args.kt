package buildsrc.utils

// https://github.com/JetBrains/kotlin/blob/8e3469f2205b19784ce9f92da29cbf86b8db5687/compiler/util-io/src/org/jetbrains/kotlin/util/Util.kt#L46-L75
fun parseSpaceSeparatedArgs(argsString: String): List<String> {
  val parsedArgs = mutableListOf<String>()
  var inQuotes = false
  var currentCharSequence = StringBuilder()
  fun saveArg(wasInQuotes: Boolean) {
    if (wasInQuotes || currentCharSequence.isNotBlank()) {
      parsedArgs.add(currentCharSequence.toString())
      currentCharSequence = StringBuilder()
    }
  }
  argsString.forEach { char ->
    when {
      char == '"'                      -> {
        inQuotes = !inQuotes
        // Save value which was in quotes.
        if (!inQuotes) {
          saveArg(true)
        }
      }

      // Space is separator.
      char.isWhitespace() && !inQuotes -> saveArg(false)

      else                             -> currentCharSequence.append(char)
    }
  }
  if (inQuotes) {
    error("No close-quote was found in $currentCharSequence.")
  }
  saveArg(false)
  return parsedArgs
}
