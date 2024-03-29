package buildsrc.settings

import java.io.File
import javax.inject.Inject
import org.gradle.api.Project
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

  private val isReleaseVersion: Provider<Boolean> =
    providers.provider { !project.version.toString().endsWith("-SNAPSHOT") }


  val sonatypeReleaseUrl: Provider<String> =
    isReleaseVersion.map { isRelease ->
      if (isRelease) {
        "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
      } else {
        "https://s01.oss.sonatype.org/content/repositories/snapshots/"
      }
    }
  val mavenCentralUsername: Provider<String> =
    d2Prop("mavenCentralUsername")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_USERNAME"))
  val mavenCentralPassword: Provider<String> =
    d2Prop("mavenCentralPassword")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_PASSWORD"))


  val jetBrainsSpaceReleaseUrl: Provider<String> =
    isReleaseVersion.map { isRelease ->
      if (isRelease) {
        "https://maven.pkg.jetbrains.space/adamkodev/p/main/maven-releases/"
      } else {
        "https://maven.pkg.jetbrains.space/adamkodev/p/main/maven-snapshots/"
      }
    }
  val jbSpaceUsername: Provider<String> =
    d2Prop("jbSpaceUsername")
      .orElse(providers.environmentVariable("MAVEN_JB_SPACE_USERNAME"))
  val jbSpacePassword: Provider<String> =
    d2Prop("jbSpacePassword")
      .orElse(providers.environmentVariable("MAVEN_JB_SPACE_PASSWORD"))


  val adamkoDevReleaseUrl: Provider<String> =
    isReleaseVersion.map { isRelease ->
      if (isRelease) {
        "https://europe-west4-maven.pkg.dev/adamko-dev/adamko-dev-releases"
      } else {
        "https://europe-west4-maven.pkg.dev/adamko-dev/adamko-dev-snapshots"
      }
    }
  val adamkoDevUsername: Provider<String> =
    d2Prop("adamkoDevUsername")
      .orElse(providers.environmentVariable("MAVEN_ADAMKO_DEV_USERNAME"))
  val adamkoDevPassword: Provider<String> =
    d2Prop("adamkoDevPassword")
      .orElse(providers.environmentVariable("MAVEN_ADAMKO_DEV_PASSWORD"))


  val signingKeyId: Provider<String> =
    d2Prop("signing.keyId")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_KEY_ID"))
  val signingKey: Provider<String> =
    d2Prop("signing.key")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_KEY"))
  val signingPassword: Provider<String> =
    d2Prop("signing.password")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_PASSWORD"))


  val githubPublishDir: Provider<File> =
    providers.environmentVariable("GITHUB_PUBLISH_DIR").map { File(it) }

  private fun d2Prop(name: String): Provider<String> =
    providers.gradleProperty("dev.adamko.dokkatoo.$name")

  private fun <T : Any> d2Prop(name: String, convert: (String) -> T): Provider<T> =
    d2Prop(name).map(convert)

  companion object {
    const val EXTENSION_NAME = "mavenPublishing"

    /** Retrieve the [MavenPublishingSettings] extension. */
    internal val Project.mavenPublishing: MavenPublishingSettings
      get() = extensions.getByType()

    /** Configure the [MavenPublishingSettings] extension. */
    internal fun Project.mavenPublishing(configure: MavenPublishingSettings.() -> Unit) =
      extensions.configure(configure)
  }
}
