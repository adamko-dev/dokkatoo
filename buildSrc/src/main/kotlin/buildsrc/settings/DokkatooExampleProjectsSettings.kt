package buildsrc.settings

import buildsrc.tasks.UpdateDokkatooExampleGradleProperties.GradlePropertiesSpec
import buildsrc.utils.adding
import buildsrc.utils.domainObjectContainer
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware

/**
 * Settings for the [buildsrc.conventions.Dokkatoo_example_projects_gradle] convention plugin
 */
abstract class DokkatooExampleProjectsSettings @Inject constructor(
  private val objects: ObjectFactory,
) : ExtensionAware {

  val gradleProperties: NamedDomainObjectContainer<GradlePropertiesSpec> =
    // create an extension so Gradle will generate DSL accessors
    extensions.adding("gradleProperties", objects.domainObjectContainer())

  companion object {
    const val EXTENSION_NAME = "dokkatooExampleProjects"
  }
}
