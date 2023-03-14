package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.Serializable
import java.net.URL
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

/**
 * Configuration builder that allows creating links leading to externally hosted
 * documentation of your dependencies.
 *
 * For instance, if you are using types from `kotlinx.serialization`, by default
 * they will be unclickable in your documentation, as if unresolved. However,
 * since API reference for `kotlinx.serialization` is also built by Dokka and is
 * [published on kotlinlang.org](https://kotlinlang.org/api/kotlinx.serialization/),
 * you can configure external documentation links for it, allowing Dokka to generate
 * documentation links for used types, making them clickable and appear resolved.
 *
 * Example in Gradle Kotlin DSL:
 *
 * ```kotlin
 * externalDocumentationLink {
 *     url.set(URL("https://kotlinlang.org/api/kotlinx.serialization/"))
 *     packageListUrl.set(
 *         rootProject.projectDir.resolve("serialization.package.list").toURL()
 *     )
 * }
 * ```
 */
abstract class DokkaExternalDocumentationLinkSpec
@DokkatooInternalApi
@Inject
constructor(
  private val name: String
) :
  DokkaParameterBuilder<DokkaParametersKxs.ExternalDocumentationLinkKxs?>,
  Serializable,
  Named {

  /**
   * Root URL of documentation to link with. **Must** contain a trailing slash.
   *
   * Dokka will do its best to automatically find `package-list` for the given URL, and link
   * declarations together.
   *
   * It automatic resolution fails or if you want to use locally cached files instead,
   * consider providing [packageListUrl].
   *
   * Example:
   *
   * ```kotlin
   * java.net.URL("https://kotlinlang.org/api/kotlinx.serialization/")
   * ```
   */
  @get:Input
  abstract val url: Property<URL>

  /**
   * Set the value of [url].
   *
   * @param[value] will be converted to a [URL]
   */
  fun url(value: String) = url.set(URL(value))

  /**
   * Set the value of [url].
   *
   * @param[value] will be converted to a [URL]
   */
  fun url(value: Provider<String>) = url.set(value.map(::URL))

  /**
   * Specifies the exact location of a `package-list` instead of relying on Dokka
   * automatically resolving it. Can also be a locally cached file to avoid network calls.
   *
   * Example:
   *
   * ```kotlin
   * rootProject.projectDir.resolve("serialization.package.list").toURL()
   * ```
   */
  @get:Input
  abstract val packageListUrl: Property<URL>

  /**
   * Set the value of [packageListUrl].
   *
   * @param[value] will be converted to a [URL]
   */
  fun packageListUrl(value: String) = packageListUrl.set(URL(value))

  /**
   * Set the value of [packageListUrl].
   *
   * @param[value] will be converted to a [URL]
   */
  fun packageListUrl(value: Provider<String>) = packageListUrl.set(value.map(::URL))

  /**
   * If enabled this link will be passed to the Dokka Generator.
   *
   * @see dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec.noStdlibLink
   * @see dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec.noJdkLink
   * @see dev.adamko.dokkatoo.dokka.parameters.DokkaSourceSetSpec.noAndroidSdkLink
   */
  @get:Input
  abstract val enabled: Property<Boolean>

  @DokkatooInternalApi
  override fun build(): DokkaParametersKxs.ExternalDocumentationLinkKxs? =
    if (enabled.getOrElse(true)) {
      DokkaParametersKxs.ExternalDocumentationLinkKxs(
        url = url.get(),
        packageListUrl = packageListUrl.get(),
      )
    } else {
      null
    }

  @Internal
  override fun getName(): String = name
}
