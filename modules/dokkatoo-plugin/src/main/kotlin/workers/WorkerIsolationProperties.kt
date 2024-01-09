package dev.adamko.dokkatoo.workers

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.process.JavaForkOptions


/**
 * Configure how a Gradle Worker is created using [org.gradle.workers.WorkerExecutor].
 */
sealed interface WorkerIsolation


/**
 * Create a Worker in the current Gradle process, with an isolated classpath.
 *
 * Presently there are no options to configure the behaviour of a classloader-isolated worker.
 *
 * @see org.gradle.workers.ClassLoaderWorkerSpec
 */
interface ClassLoaderIsolation : WorkerIsolation {
  // no options yet...
}


/**
 * Create a Worker using process isolation.
 *
 * Gradle will launch new Java process specifically for Dokka.
 *
 * @see org.gradle.workers.ProcessWorkerSpec
 */
interface ProcessIsolation : WorkerIsolation {
  /** @see JavaForkOptions.setDebug */
  @get:Input
  @get:Optional
  val debug: Property<Boolean>

  /** @see JavaForkOptions.setEnableAssertions */
  @get:Input
  @get:Optional
  val enableAssertions: Property<Boolean>

  /** @see JavaForkOptions.setMinHeapSize */
  @get:Input
  @get:Optional
  val minHeapSize: Property<String>

  /** @see JavaForkOptions.setMaxHeapSize */
  @get:Input
  @get:Optional
  val maxHeapSize: Property<String>

  /** @see JavaForkOptions.setJvmArgs */
  @get:Input
  @get:Optional
  val jvmArgs: ListProperty<String>

  /** @see JavaForkOptions.setAllJvmArgs */
  @get:Input
  @get:Optional
  val allJvmArgs: ListProperty<String>

  /** @see JavaForkOptions.setDefaultCharacterEncoding */
  @get:Input
  @get:Optional
  val defaultCharacterEncoding: Property<String>

  /** @see JavaForkOptions.setSystemProperties */
  @get:Input
  @get:Optional
  val systemProperties: MapProperty<String, Any>
}
