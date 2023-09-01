package buildsrc.settings

import java.io.File
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*


/**
 * Settings for the [buildsrc.conventions.Maven_publish_test_gradle] convention plugin.
 */
abstract class MavenPublishingSettings @Inject constructor(
  private val project: Project,
  private val providers: ProviderFactory,
) {

  private val isReleaseVersion =
    providers.provider { !project.version.toString().endsWith("-SNAPSHOT") }

  val sonatypeReleaseUrl = isReleaseVersion.map { isRelease ->
    if (isRelease) {
      "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
    } else {
      "https://oss.sonatype.org/content/repositories/snapshots/"
    }
  }

  private val mavenCentralUsername = d2Prop("mavenCentralUsername")
    .orElse(providers.environmentVariable("MAVEN_SONATYPE_USERNAME"))
  private val mavenCentralPassword = d2Prop("mavenCentralPassword")
    .orElse(providers.environmentVariable("MAVEN_SONATYPE_PASSWORD"))

  val mavenCentralCredentials =
    providers.zip(mavenCentralUsername, mavenCentralPassword) { user, pass ->
      Action<PasswordCredentials> {
        username = user
        password = pass
      }
    }

  val signingKeyId = d2Prop("signing.keyId")
    .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_KEY_ID"))
  val signingKey = d2Prop("signing.key")
    .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_KEY"))
  val signingPassword = d2Prop("signing.password")
    .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_PASSWORD"))

  val githubPublishDir: Provider<File> =
    providers.environmentVariable("GITHUB_PUBLISH_DIR").map { File(it) }

  private fun d2Prop(name: String): Provider<String> =
    providers.gradleProperty("dev.adamko.dokkatoo.$name")

  private fun <T : Any> d2Prop(name: String, convert: (String) -> T): Provider<T> =
    d2Prop(name).map(convert)

  companion object {
    const val EXTENSION_NAME = "mavenPublishing"

    /** Retrieve the [KayrayBuildProperties] extension. */
    internal val Project.mavenPublishing: MavenPublishingSettings
      get() = extensions.getByType()

    /** Configure the [KayrayBuildProperties] extension. */
    internal fun Project.mavenPublishing(configure: MavenPublishingSettings.() -> Unit) =
      extensions.configure(configure)
  }
}
