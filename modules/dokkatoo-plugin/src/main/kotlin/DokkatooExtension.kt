package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetGradleBuilder
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property

/**
 * Configure the behaviour of the [DokkatooBasePlugin].
 */
abstract class DokkatooExtension: ExtensionAware {

  /** Directory into which [DokkaPublication]s will be produced */
  abstract val dokkatooPublicationDirectory: DirectoryProperty

  /** Directory into which Dokka Modules will be produced */
  abstract val dokkatooModuleDirectory: DirectoryProperty

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

  interface Versions {

    /** Default version used for Dokka dependencies */
      val jetbrainsDokka: Property<String>
      val jetbrainsMarkdown: Property<String>
      val freemarker: Property<String>
      val kotlinxHtml: Property<String>
  }
}
