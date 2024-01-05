@file:Suppress("unused")

package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Deprecated: DokkatooPrepareModuleDescriptorTask was not compatible with relocatable Gradle Build Cache and has been replaced with a dark Gradle devilry. All references to DokkatooPrepareModuleDescriptorTask must be removed.
 *
 * Produces a Dokka Configuration that describes a single module of a multimodule Dokka configuration.
 */
@DisableCachingByDefault
@Deprecated("DokkatooPrepareModuleDescriptorTask was not compatible with relocatable Gradle Build Cache and has been replaced with a dark Gradle devilry. All references to DokkatooPrepareModuleDescriptorTask must be removed.")
abstract class DokkatooPrepareModuleDescriptorTask
@DokkatooInternalApi
@Inject
constructor() : DokkatooTask() {

  @get:Internal
  abstract val dokkaModuleDescriptorJson: RegularFileProperty

  @get:Internal
  abstract val moduleName: Property<String>

  @get:Internal
  abstract val modulePath: Property<String>

  @get:Internal
  abstract val moduleDirectory: DirectoryProperty

  @get:Internal
  abstract val includes: ConfigurableFileCollection

  @TaskAction
  internal fun generateModuleConfiguration() {
    logger.warn("DokkatooPrepareModuleDescriptorTask was not compatible with relocatable Gradle Build Cache and has been replaced with a dark Gradle devilry. All references to DokkatooPrepareModuleDescriptorTask must be removed.")
  }
}
