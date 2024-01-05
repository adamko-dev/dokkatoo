package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.HasFormatName

@DokkatooInternalApi
class TaskNames(override val formatName: String?) : HasFormatName() {
  val generate = "dokkatooGenerate".appendFormat()
  val generatePublication = "dokkatooGeneratePublication".appendFormat()
  val generateModule = "dokkatooGenerateModule".appendFormat()
  @Deprecated("DokkatooPrepareModuleDescriptorTask was not compatible with relocatable Gradle Build Cache and has been replaced with a dark Gradle devilry. All references to DokkatooPrepareModuleDescriptorTask must be removed.")
  val prepareModuleDescriptor = "prepareDokkatooModuleDescriptor".appendFormat()
}
