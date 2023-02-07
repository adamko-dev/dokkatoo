package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask

/** Base Dokkatoo task */
@CacheableTask
abstract class DokkatooTask : DefaultTask() {

  init {
    group = DokkatooPlugin.TASK_GROUP
  }

}
