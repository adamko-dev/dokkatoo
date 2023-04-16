package dev.adamko.dokkatoo.internal

import dev.adamko.dokkatoo.DokkatooExtension
import org.gradle.kotlin.dsl.*

// When Dokkatoo is applied to a build script Gradle will auto-generate these accessors

internal val DokkatooExtension.versions: DokkatooExtension.Versions
  get() = extensions.getByType()


internal fun DokkatooExtension.versions(configure: DokkatooExtension.Versions.() -> Unit) {
  versions.apply(configure)
}
