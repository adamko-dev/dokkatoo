package buildsrc.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*

/**
 * Utility for updating the `gradle.properties` of projects used in automated tests.
 */
@CacheableTask
abstract class UpdateDokkatooExampleGradleProperties @Inject constructor(
  @get:Internal
  val objects: ObjectFactory
) : DefaultTask() {

  @get:Nested
  abstract val gradleProperties: NamedDomainObjectContainer<GradlePropertiesSpec>

  @TaskAction
  fun update() {
    gradleProperties.forEach { spec ->

      val content = buildString {
        appendLine("# DO NOT EDIT - Generated by ${this@UpdateDokkatooExampleGradleProperties.path}")
        appendLine()

        spec.content.orNull?.sorted()?.forEach {
          appendLine(it)
        }
      }

      spec.gradlePropertiesFile.get().asFile.writeText(content)
    }
  }

  /**
   * Represents a `gradle.properties` file that should be automatically generated,
   * into the file [gradleProperties].
   */
  abstract class GradlePropertiesSpec(
    private val name: String
  ) : Named {

    /** The generated `gradle.properties` file */
    @get:OutputFile
    val gradlePropertiesFile: Provider<RegularFile> = destinationDir.file("gradle.properties")

    /** The directory that the `gradle.properties` will be generated into */
    @get:Internal
    abstract val destinationDir: DirectoryProperty

    /**
     * Content to add to the `gradle.properties` file.
     *
     * Elements may span multiple lines.
     *
     * Elements will be sorted before appending to the file (to improve caching & reproduciblity).
     */
    @get:Input
    @get:Optional
    abstract val content: ListProperty<String>

    @Input
    override fun getName(): String = name
  }
}