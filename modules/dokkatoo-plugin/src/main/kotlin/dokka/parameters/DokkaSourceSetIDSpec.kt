package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.DokkaSourceSetID

abstract class DokkaSourceSetIDSpec
@DokkatooInternalApi
@Inject
constructor(
  /**
   * Unique identifier of the scope that this source set is placed in.
   * Each scope provide only unique source set names.
   *
   * TODO update this doc - DokkaTask doesn't represent one source set scope anymore
   *
   * E.g. One DokkaTask inside the Gradle plugin represents one source set scope, since there cannot be multiple
   * source sets with the same name. However, a Gradle project will not be a proper scope, since there can be
   * multiple DokkaTasks that contain source sets with the same name (but different configuration)
   */
  @get:Input
  val scopeId: String
) : DokkaParameterBuilder<DokkaSourceSetID>, Named {

  @get:Input
  abstract var sourceSetName: String

  @DokkatooInternalApi
  override fun build(): DokkaSourceSetID = DokkaSourceSetID(scopeId, sourceSetName)

  @Internal
  override fun getName(): String = scopeId

}
