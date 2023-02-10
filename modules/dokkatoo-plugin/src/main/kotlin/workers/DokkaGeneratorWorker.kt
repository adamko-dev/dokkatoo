package dev.adamko.dokkatoo.workers

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.LoggerAdapter
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
abstract class DokkaGeneratorWorker : WorkAction<DokkaGeneratorWorker.Parameters> {

  private val logger = LoggerAdapter(DokkaGeneratorWorker::class)

  interface Parameters : WorkParameters {
    val dokkaConfiguration: Property<DokkaConfiguration>
  }

  @OptIn(ExperimentalTime::class)
  override fun execute() {
    val dokkaConfiguration = parameters.dokkaConfiguration.get()
    logger.info("Executing DokkaGeneratorWorker with dokkaConfiguration: $dokkaConfiguration")

    val generator = DokkaGenerator(dokkaConfiguration, logger)

    // can't use kotlin.time.measureTime {} because the implementation isn't stable across Kotlin versions
    val duration = System.nanoTime().let { startTime ->
      generator.generate()
      (System.nanoTime() - startTime).nanoseconds
    }

    logger.info("DokkaGeneratorWorker completed in $duration")
  }
}
