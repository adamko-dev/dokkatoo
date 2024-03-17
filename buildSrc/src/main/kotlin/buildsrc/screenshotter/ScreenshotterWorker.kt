package buildsrc.screenshotter

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Browser.NewPageOptions
import com.microsoft.playwright.Page.ScreenshotOptions
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.ColorScheme
import com.microsoft.playwright.options.ColorScheme.DARK
import com.microsoft.playwright.options.ColorScheme.LIGHT
import com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED
import java.io.Serializable
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

internal abstract class ScreenshotterWorker : WorkAction<ScreenshotterWorker.Parameters> {

  private val logger = Logging.getLogger(ScreenshotterWorker::class.java)

  interface Parameters : WorkParameters {
    val outputDirectory: DirectoryProperty
    val websites: ListProperty<Website>

    data class Website(
      val name: String,
      val uri: URI,
      val isEnabled: Boolean,
    ) : Serializable {
      constructor(website: buildsrc.screenshotter.Website) : this(
        name = website.name,
        uri = website.uri.get(),
        isEnabled = website.enabled.orNull ?: false,
      )
    }
  }

  override fun execute() {
    Playwright.create().use { playwright ->
      logger.info("[ScreenshotterWorker] Created Playwright ${playwright}")
      playwright.webkit().launch().use { browser ->
        parameters.websites
          .get()
          .filter { it.isEnabled }
          .forEach { website ->
            browser.screenshot(website.name, website.uri, LIGHT)
            browser.screenshot(website.name, website.uri, DARK)
          }
      }
    }
  }

  private fun Browser.screenshot(
    name: String,
    uri: URI,
    colorScheme: ColorScheme,
  ) {
    val outputFileName = "$name-${colorScheme.name.lowercase()}.png"
    val outputFile = parameters.outputDirectory
      .file(outputFileName)
      .get().asFile
    val duration = measureTime {
      newPage(
        NewPageOptions()
          .setColorScheme(colorScheme)
      ).apply {
        navigate(uri.toString()).finished()
        waitForLoadState(DOMCONTENTLOADED)
        Thread.sleep(0.5.seconds.inWholeMilliseconds)
        screenshot(
          ScreenshotOptions().setPath(outputFile.toPath())
        )
      }
    }
    logger.lifecycle("[ScreenshotterWorker] Captured ${colorScheme.name.lowercase()} screenshot for $name $uri in $duration")
  }

  companion object {
    // can't use kotlin.time.measureTime {} because Gradle forces the language level to be low.
    private fun measureTime(block: () -> Unit): Duration =
      System.nanoTime().let { startTime ->
        block()
        (System.nanoTime() - startTime).nanoseconds
      }
  }
}
