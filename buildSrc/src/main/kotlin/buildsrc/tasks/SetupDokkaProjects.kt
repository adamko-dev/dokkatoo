package buildsrc.tasks

import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

abstract class SetupDokkaProjects @Inject constructor(
  private val files: FileSystemOperations,
  private val layout: ProjectLayout,
  private val objects: ObjectFactory,
) : DefaultTask() {

  @get:Input
  abstract val destinationToSources: MapProperty<File, List<String>>

  @get:InputDirectory
  abstract val dokkaSourceDir: DirectoryProperty

  @get:OutputDirectories
  val destinationDirs: FileCollection = layout.files(
    destinationToSources.map { it.keys }
  )

  init {
    group = "dokka examples"
  }

  @TaskAction
  internal fun action() {
    val destinationToSources = destinationToSources.get()
    val dokkaSourceDir = dokkaSourceDir.get()

    logger.info("destinationToSources: $destinationToSources")

    destinationToSources.forEach { (dest: File, sources: List<String>) ->
      files.sync {
        sources.forEach { src ->
          from(dokkaSourceDir.dir(src))
        }
        into(dest)
      }
    }
  }
}
