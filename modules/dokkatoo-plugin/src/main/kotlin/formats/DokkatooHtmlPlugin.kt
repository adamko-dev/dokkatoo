package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.internal.DokkatooInternalApi

abstract class DokkatooHtmlPlugin
@DokkatooInternalApi
constructor() : DokkatooFormatPlugin(formatName = "html") {
  // HTML is the default - no special config needed!
}
