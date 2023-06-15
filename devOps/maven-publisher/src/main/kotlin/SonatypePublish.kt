import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.TaskAction

abstract class SonatypePublish @Inject constructor(
  private val fs: FileSystemOperations,
) : DefaultTask() {

  @get:InputDirectory
  abstract val localRepoDir: DirectoryProperty

  @get:LocalState
  abstract val workingDir: DirectoryProperty

  @TaskAction
  fun publish() {
//    val workingDir = workingDir.get().asFile
//    val localRepoDir = localRepoDir.get().asFile

//    fs.delete { delete(workingDir) }
//    workingDir.mkdirs()

    fs.sync {
      from(localRepoDir)
      into(workingDir)
    }

  }
}
