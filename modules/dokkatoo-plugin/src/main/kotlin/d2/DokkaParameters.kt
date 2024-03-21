package dev.adamko.dokkatoo.d2

import dev.adamko.dokkatoo.dokka.parameters.*
import org.gradle.api.DomainObjectSet
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty


/**
 * Specific per project. Sharable.
 */
internal interface DokkaBaseParameters {
  val moduleName: Property<String>
  val moduleVersion: Property<String>
  val suppressObviousFunctions: Property<Boolean>
  val suppressInheritedMembers: Property<Boolean>
  val failOnWarning: Property<Boolean>
}

/**
 * Specific per project. Non-shareable.
 */
internal interface DokkaEngineOptions {
  val outputDirectory: DirectoryProperty
  val offlineMode: Property<Boolean>
}


/**
 * Unprocessed files, which will be passed into Dokka Engine.
 *
 * Shareable.
 */
internal sealed interface DokkaResource {
  interface Samples : DokkaResource
  interface SourceSet : DokkaResource
  interface SourceSetDependencies : DokkaResource
  interface Includes : DokkaResource
  interface DokkaParameters : DokkaResource
  interface ModuleParameters : DokkaResource
  interface EnginePluginParameters : DokkaResource
}


internal interface DokkaSourceSet {
  val suppress: Property<Boolean>
  val analysisPlatform: Property<KotlinPlatform>
  val kotlinApiVersion: Property<String?>
  val dependentSourceSets: NamedDomainObjectContainer<DokkaSourceSetIdSpec>
  val displayName: Property<String>
  val documentedVisibilities: SetProperty<VisibilityModifier>
  val enableAndroidDocumentationLink: Property<Boolean>
  val enableJdkDocumentationLink: Property<Boolean>
  val enableKotlinStdLibDocumentationLink: Property<Boolean>
  val externalDocumentationLinks: NamedDomainObjectContainer<DokkaExternalDocumentationLinkSpec>
  val jdkVersion: Property<Int>
  val languageVersion: Property<String?>
  val perPackageOptions: DomainObjectSet<DokkaPackageOptionsSpec>
  val reportUndocumented: Property<Boolean>
  val samples: ConfigurableFileCollection
  val skipDeprecated: Property<Boolean>
  val skipEmptyPackages: Property<Boolean>
  val suppressGeneratedFiles: Property<Boolean>
  val sourceSetId: Provider<DokkaSourceSetIdSpec>
  val sourceSetScope: Property<String>
  val sourceLinks: DomainObjectSet<DokkaSourceLinkSpec>

  val sourceRoots: ConfigurableFileCollection
  val includes: ConfigurableFileCollection
  val classpath: ConfigurableFileCollection

  val suppressedFiles: ConfigurableFileCollection
}
