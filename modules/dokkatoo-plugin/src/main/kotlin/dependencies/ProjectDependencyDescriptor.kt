package dev.adamko.dokkatoo.dependencies

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.api.Named
import org.gradle.api.provider.SetProperty

@DokkatooInternalApi
abstract class ProjectDependencyDescriptor(
  val projectPath: String,
) : Named {

  abstract val dokkaFormats: SetProperty<String>

  override fun getName(): String = projectPath
}
