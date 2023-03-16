package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested

/** Base Dokkatoo task */
@CacheableTask
abstract class DokkatooTask
@DokkatooInternalApi
constructor() : DefaultTask() {

  init {
    group = DokkatooBasePlugin.TASK_GROUP
  }

  /**
   * A [DokkatooTask] that depends on [DokkaSourceSetSpec]s.
   *
   * Any task that implements [WithSourceSets] doesn't have to use the sources, but it will use
   * them as part of Gradle up-to-date checks.
   */
  @CacheableTask
  abstract class WithSourceSets
  @DokkatooInternalApi
  constructor() : DokkatooTask() {

    /**
     * Source sets used to generate a Dokka Module.
     *
     * The values are not used directly in this task, but they are required to be registered as a
     * task input for up-to-date checks
     */
    @get:Nested
    abstract val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec>

    fun addAllDokkaSourceSets(sourceSets: Provider<Iterable<DokkaSourceSetSpec>>) {
      dokkaSourceSets.addAllLater(sourceSets)
    }
  }
}
