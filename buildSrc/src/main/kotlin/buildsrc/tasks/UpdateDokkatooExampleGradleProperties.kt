package buildsrc.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*

/**
 * Utility for updating the `gradle.properties` of projects used in automated tests.
 */
@CacheableTask
abstract class UpdateDokkatooExampleGradleProperties @Inject constructor(
  @get:Internal
  val objects: ObjectFactory
) : DefaultTask(), ExtensionAware {

  @get:Nested
  abstract val gradleProperties: NamedDomainObjectContainer<GradlePropertiesSpec>

  @get:Internal // tracked by testMavenRepoPath (don't care about the directory contents, only the path)
  abstract val testMavenRepo: DirectoryProperty

  @get:Input
  protected val testMavenRepoPath: Provider<String> =
    testMavenRepo.asFile.map { it.invariantSeparatorsPath }

  @TaskAction
  fun update() {
    gradleProperties.forEach { spec ->

      val content = buildString {
        appendLine("# DO NOT EDIT - Generated by ${this@UpdateDokkatooExampleGradleProperties.path}")
        appendLine()
        if (spec.enableTestMavenRepo.get()) {
          appendLine("testMavenRepo=${testMavenRepoPath.get()}")
          appendLine()
        }
        spec.content.orNull?.takeIf { it.isNotBlank() }?.let { content ->
          appendLine(content)
          appendLine()
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

    /** Optional additional content to append to the `gradle.properties` file */
    @get:Input
    @get:Optional
    abstract val content: Property<String>

    /** Whether the project-local Maven repo (that contains Dokkatoo snapshots) should be added */
    @get:Input
    abstract val enableTestMavenRepo: Property<Boolean>

    @Input
    override fun getName(): String = name
  }
}
