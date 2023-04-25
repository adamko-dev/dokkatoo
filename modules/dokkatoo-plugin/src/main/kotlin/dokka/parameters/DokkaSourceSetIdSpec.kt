package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.*

abstract class DokkaSourceSetIdSpec
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
) : DokkaParameterBuilder<DokkaParametersKxs.SourceSetIdKxs>, Named, Serializable {

  @get:Input
  abstract var sourceSetName: String

  @DokkatooInternalApi
  override fun build(): DokkaParametersKxs.SourceSetIdKxs =
    DokkaParametersKxs.SourceSetIdKxs(scopeId, sourceSetName)

  @Internal
  override fun getName(): String = "$scopeId/$sourceSetName"

  override fun toString(): String = "DokkaSourceSetIdSpec($name)"

  companion object {

    /** Utility for creating a new [DokkaSourceSetIdSpec] instance using [ObjectFactory.newInstance] */
    @DokkatooInternalApi
    fun ObjectFactory.dokkaSourceSetIdSpec(
      scopeId: String,
      sourceSetName: String,
    ): DokkaSourceSetIdSpec =
      newInstance<DokkaSourceSetIdSpec>(scopeId).apply {
        this.sourceSetName = sourceSetName
      }
  }
}
