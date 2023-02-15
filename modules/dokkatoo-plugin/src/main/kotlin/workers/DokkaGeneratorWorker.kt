package dev.adamko.dokkatoo.workers

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.LoggerAdapter
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.nanoseconds
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator

/**
 * Gradle Worker Daemon for running [DokkaGenerator].
 *
 * The worker requires [DokkaGenerator] is present on the classpath.
 */
@DokkatooInternalApi
@OptIn(ExperimentalTime::class)
abstract class DokkaGeneratorWorker : WorkAction<DokkaGeneratorWorker.Parameters> {

  private val logger = LoggerAdapter(DokkaGeneratorWorker::class)

  interface Parameters : WorkParameters {
    val dokkaParameters: Property<DokkaConfiguration>
  }

  override fun execute() {
    val dokkaParameters = parameters.dokkaParameters.get()
    logger.progress("Executing DokkaGeneratorWorker with dokkaParameters: $dokkaParameters")

    val generator = DokkaGenerator(dokkaParameters, logger)

    val duration = measureTime { generator.generate() }

    logger.info("DokkaGeneratorWorker completed in $duration")
  }

  companion object {
    // can't use kotlin.time.measureTime {} because the implementation isn't stable across Kotlin versions
    private fun measureTime(block: () -> Unit): Duration =
      System.nanoTime().let { startTime ->
        block()
        (System.nanoTime() - startTime).nanoseconds
      }
  }
}
