package buildsrc.conventions

import buildsrc.services.GitService
import buildsrc.tasks.DokkatooReleaseTask
import org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_TASK_GROUP

//
//
//private fun Gradle.registerGitService(
//  settings: VcsMvnPublishSettings
//): Provider<GitService> {
//  return
//}
//
//
val gitServiceProvider =
  gradle.sharedServices.registerIfAbsent(
    GitService.NAME,
    GitService::class,
  ) {
    maxParallelUsages.set(1)

    parameters {
      gitExec.convention("git")
      defaultOrigin.convention("origin")
      gitDirFlagEnabled.convention(true)
    }
  }

val releaseDokkatoo by tasks.registering(DokkatooReleaseTask::class) {
  group = PUBLISH_TASK_GROUP
  description = "Automatically publish a new Dokkatoo release"

}
