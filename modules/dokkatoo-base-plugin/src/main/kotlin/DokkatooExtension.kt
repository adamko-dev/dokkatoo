package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetGradleBuilder
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/**
 * Configure the behaviour of the [DokkatooPlugin].
 */
abstract class DokkatooExtension {

  /** Default version used for Dokka dependencies */
  abstract val dokkaVersion: Property<String>

  /** Directory into which [DokkaPublication]s will be produced */
  abstract val dokkatooPublicationDirectory: DirectoryProperty

  abstract val dokkatooConfigurationsDirectory: DirectoryProperty

  /** Default Dokkatoo cache directory */
  abstract val dokkatooCacheDirectory: DirectoryProperty

  abstract val moduleNameDefault: Property<String>
  abstract val moduleVersionDefault: Property<String>

  /**
   * String used to discriminate between source sets that originate from different Gradle subprojects
   *
   * Defaults to [the path of the subproject][org.gradle.api.Project.getPath].
   */
  abstract val sourceSetScopeDefault: Property<String>

  /**
   * Configuration for creating Dokka Publications.
   *
   * Each publication will generate one Dokka site based on the included Dokka Source Sets.
   *
   * The type of site is determined by the Dokka Plugins. By default, an HTML site will be generated.
   */
  abstract val dokkatooPublications: NamedDomainObjectContainer<DokkaPublication>

  /**
   * Dokkatoo Source Sets that describe source code in the local project (not other subprojects).
   *
   * These source sets will be added to all [dokkatooPublications].
   */
  abstract val dokkatooSourceSets: NamedDomainObjectContainer<DokkaSourceSetGradleBuilder>
}
