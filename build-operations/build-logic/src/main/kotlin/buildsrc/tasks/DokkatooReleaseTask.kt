package buildsrc.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_TASK_GROUP
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class DokkatooReleaseTask @Inject constructor(
  private val exec: ExecOperations,
) : DefaultTask() {

  @TaskAction
  fun execRelease() {

  }

}
