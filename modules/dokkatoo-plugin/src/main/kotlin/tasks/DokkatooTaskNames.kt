package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.formats.HasDokkatooFormatName
import dev.adamko.dokkatoo.internal.DokkatooInternalApi


@DokkatooInternalApi
class DokkatooTaskNames(override val formatName: String?) : HasDokkatooFormatName {
  val generate = "dokkatooGenerate".appendFormat()
  val generatePublication = "dokkatooGeneratePublication".appendFormat()
  val generateModule = "dokkatooGenerateModule".appendFormat()
  val prepareParameters = "prepareDokkatooParameters".appendFormat()
  val prepareModuleDescriptor = "prepareDokkatooModuleDescriptor".appendFormat()
}
