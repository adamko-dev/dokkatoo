package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask

/** Base Dokka task */
@CacheableTask
abstract class DokkatooTask : DefaultTask() {

  init {
    group = DokkatooPlugin.TASK_GROUP
  }

}
