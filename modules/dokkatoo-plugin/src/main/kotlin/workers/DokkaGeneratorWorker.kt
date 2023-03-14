package dev.adamko.dokkatoo.workers

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.LoggerAdapter
import java.time.Duration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator

/**
 * Gradle Worker Daemon for running [DokkaGenerator].
 *
 * The worker requires [DokkaGenerator] is present on its classpath, as well as any Dokka plugins
 * that are used to generate the Dokka files. Transitive dependencies are also required.
 */
@DokkatooInternalApi
abstract class DokkaGeneratorWorker : WorkAction<DokkaGeneratorWorker.Parameters> {

  @DokkatooInternalApi
  interface Parameters : WorkParameters {
    val dokkaParameters: Property<DokkaConfiguration>
    val logFile: RegularFileProperty
  }

  override fun execute() {
    LoggerAdapter(parameters.logFile.get().asFile).use { logger ->

      val dokkaParameters = parameters.dokkaParameters.get()
      logger.progress("Executing DokkaGeneratorWorker with dokkaParameters: $dokkaParameters")

      val generator = DokkaGenerator(dokkaParameters, logger)

      val duration = measureTime { generator.generate() }

      logger.info("DokkaGeneratorWorker completed in $duration")
    }
  }

  @DokkatooInternalApi
  companion object {
    // can't use kotlin.Duration or kotlin.time.measureTime {} because
    // the implementation isn't stable across Kotlin versions
    private fun measureTime(block: () -> Unit): Duration =
      System.nanoTime().let { startTime ->
        block()
        Duration.ofNanos(System.nanoTime() - startTime)
      }
  }
}
