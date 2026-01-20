package dev.adamko.dokkatoo.services

import dev.adamko.dokkatoo.dependencies.ProjectDependencyDescriptor
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

@DokkatooInternalApi
abstract class DokkatooBuildService : BuildService<DokkatooBuildService.Parameters> {

  @DokkatooInternalApi
  interface Parameters : BuildServiceParameters

  abstract val projects: NamedDomainObjectContainer<ProjectDependencyDescriptor>

  @DokkatooInternalApi
  companion object {
    const val DBS_NAME = "dokkatooBuildService"
  }
}
