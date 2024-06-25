package dev.adamko.dokkatoo.utils

import io.kotest.matchers.string.shouldContain
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.toPath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.gradle.testkit.runner.BuildResult


fun BuildResult.configurationCacheReport(): ConfigurationCacheReport {
  output shouldContain "See the complete report at"

  val reportPath = output.lines()
    .first { it.startsWith("See the complete report at") }
    .substringAfter("See the complete report at")
    .let { URI(it.trim()).toPath() }

  return parseReport(reportPath)
}

private fun parseReport(
  path: Path
): ConfigurationCacheReport {
  val data = path.bufferedReader()
    .lineSequence()
    .dropWhile { !it.matches("// *begin-report-data.*".toRegex()) }
    .drop(1)
    .takeWhile { !it.matches("// *end-report-data.*".toRegex()) }
    .joinToString("\n")

  return Json.decodeFromString(ConfigurationCacheReport.serializer(), data)
}

@Serializable
data class ConfigurationCacheReport(
  val diagnostics: JsonArray,
  val totalProblemCount: Int,
  val cacheAction: String,
  val requestedTasks: String,
  val documentationLink: String,
)
