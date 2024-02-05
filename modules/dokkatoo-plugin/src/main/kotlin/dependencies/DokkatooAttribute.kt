package dev.adamko.dokkatoo.dependencies

import dev.adamko.dokkatoo.internal.Attribute
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute

/**
 * Gradle Configuration Attributes for sharing Dokkatoo files across subprojects.
 *
 * These attributes are used to tag [Configuration]s, so files can be shared between subprojects.
 */
@DokkatooInternalApi
interface DokkatooAttribute {

  /** HTML, Markdown, etc. */
  @DokkatooInternalApi
  interface Format : Named

  /** Generated output, or subproject classpath, or included files, etc */
  @DokkatooInternalApi
  interface ModuleComponent : Named

  /** A classpath, e.g. for Dokka Plugins or the Dokka Generator. */
  @DokkatooInternalApi
  interface Classpath : Named

  @DokkatooInternalApi
  companion object {
    val DokkatooFormatAttribute: Attribute<Format> =
      Attribute("dev.adamko.dokkatoo.format")

    val DokkatooModuleComponentAttribute: Attribute<ModuleComponent> =
      Attribute("dev.adamko.dokkatoo.module-component")

    val DokkatooClasspathAttribute: Attribute<Classpath> =
      Attribute("dev.adamko.dokkatoo.classpath")
  }
}
