package buildsrc.screenshotter

import buildsrc.utils.domainObjectContainer
import java.net.URI
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.workers.WorkerExecutor

/**
 * Take screenshots of [websites] using Playwright.
 */
@CacheableTask
abstract class ScreenshotTask @Inject constructor(
  private val workers: WorkerExecutor,
  private val objects: ObjectFactory,
) : DefaultTask() {

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @get:Nested
  val websites: NamedDomainObjectContainer<Website> = objects.domainObjectContainer()

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  init {
    group = "screenshotter"
    description = "take screenshots of websites, save to directory"
    extensions.add("websites", websites) // so that Gradle generates Kotlin DSL accessors
  }

  @TaskAction
  fun action() {
    val workQueue = workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }

    workQueue.submit(ScreenshotterWorker::class) {
      this.websites.addAll(this@ScreenshotTask.websites.map(ScreenshotterWorker.Parameters::Website))
      this.outputDirectory = this@ScreenshotTask.outputDirectory
    }
  }

  fun website(name: String, uri: String) {
    websites.register(name) {
      this.uri.set(URI(uri))
    }
  }
}
