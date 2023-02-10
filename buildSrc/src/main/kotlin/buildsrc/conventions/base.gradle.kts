package buildsrc.conventions

import buildsrc.conventions.utils.asConsumer
import buildsrc.conventions.utils.asProvider
import buildsrc.conventions.utils.dropDirectories
import java.time.Duration
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  base
}

// common config for all projects

if (project != rootProject) {
  project.version = rootProject.version
  project.group = rootProject.group
}

tasks.withType<AbstractArchiveTask>().configureEach {
  // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}

tasks.withType<AbstractTestTask>().configureEach {
  timeout.set(Duration.ofMinutes(10))

  testLogging {
    showCauses = true
    showExceptions = true
    showStackTraces = true
    showStandardStreams = true
    events(
      TestLogEvent.PASSED,
      TestLogEvent.FAILED,
      TestLogEvent.SKIPPED,
      TestLogEvent.STARTED,
      TestLogEvent.STANDARD_ERROR,
      TestLogEvent.STANDARD_OUT,
    )
  }
}

tasks.withType<AbstractCopyTask>().configureEach {
  includeEmptyDirs = false
}


val kotlinDokkaSource by configurations.registering {
  asConsumer()
  attributes {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named("externals-dokka-src"))
  }
}

val kotlinDokkaSourceElements by configurations.registering {
  asProvider()
  attributes {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named("externals-dokka-src"))
  }
}
