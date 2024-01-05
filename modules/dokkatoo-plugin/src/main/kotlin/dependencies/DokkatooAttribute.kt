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

  /** HTML, Markdown, etc */
  @DokkatooInternalApi
  interface Format : Named

  /** Dokka module name, set in the DSL of the subproject */
  @DokkatooInternalApi
  interface ModuleName : Named

  /** Project path e.g. `:x:y:z:some-subproject` */
  @DokkatooInternalApi
  interface ModulePath : Named

  /** Generated output, or subproject classpath, or included files, etc */
  @DokkatooInternalApi
  interface ModuleComponent : Named

  /** A classpath, e.g. for Dokka Plugins or the Dokka Generator. */
  @DokkatooInternalApi
  interface Classpath : Named

  /** Full path of a Gradle task used to generate a Dokka Module. */
  // ugly hack workaround for https://github.com/gradle/gradle/issues/13590
  @DokkatooInternalApi
  interface ModuleGenerateTaskPath : Named

  @DokkatooInternalApi
  companion object {
    val DokkatooFormatAttribute: Attribute<Format> =
      Attribute("dev.adamko.dokkatoo.format")

    val DokkatooModuleNameAttribute: Attribute<ModuleName> =
      Attribute("dev.adamko.dokkatoo.module-name")

    val DokkatooModulePathAttribute: Attribute<ModulePath> =
      Attribute("dev.adamko.dokkatoo.module-path")

    // ugly hack workaround for https://github.com/gradle/gradle/issues/13590
    val DokkatooModuleGenerateTaskPathAttribute: Attribute<ModuleGenerateTaskPath> =
      Attribute("dev.adamko.dokkatoo.module-generate-task-path")

    val DokkatooModuleComponentAttribute: Attribute<ModuleComponent> =
      Attribute("dev.adamko.dokkatoo.module-component")

    val DokkatooClasspathAttribute: Attribute<Classpath> =
      Attribute("dev.adamko.dokkatoo.classpath")
  }
}
