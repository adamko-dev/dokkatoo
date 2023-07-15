package dokkatoo.utils

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.*


fun RepositoryHandler.gitHubRelease(): IvyArtifactRepository {
  return ivy("https://github.com/") {
    name = "GitHub Release"
    patternLayout {
      artifact("[organization]/[module]/archive/[revision].[ext]")
      artifact("[organization]/[module]/archive/refs/tags/[revision].[ext]")
      artifact("[organization]/[module]/archive/refs/tags/v[revision].[ext]")
    }
    metadataSources { artifact() }
  }
}


//region JetBrains repositories
val RepositoryHandler.jetBrains: JetBrainsRepositories
  get() = JetBrainsRepositories(this)

fun RepositoryHandler.jetBrains(block: JetBrainsRepositories.() -> Unit): JetBrainsRepositories {
  return jetBrains.apply(block)
}

class JetBrainsRepositories(private val repositories: RepositoryHandler) {

  /** Add the JetBrains Maven Repository `https://www.jetbrains.com/intellij-repository/snapshots` */
  fun intellijRepositorySnapshots(
    configure: MavenArtifactRepository.() -> Unit = {}
  ): MavenArtifactRepository =
    repositories.maven("https://www.jetbrains.com/intellij-repository/snapshots") {
      mavenContent { snapshotsOnly() }
      apply(configure)
    }

  /** Add the JetBrains Maven Repository `https://www.jetbrains.com/intellij-repository/releases` */
  fun intellijRepositoryReleases(
    configure: MavenArtifactRepository.() -> Unit = {}
  ): MavenArtifactRepository =
    repositories.maven("https://www.jetbrains.com/intellij-repository/releases") {
      mavenContent { releasesOnly() }
      apply(configure)
    }

  /** Add the JetBrains Maven Repository `https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide` */
  fun kotlinIde(
    configure: MavenArtifactRepository.() -> Unit = {}
  ): MavenArtifactRepository =
    repositories.maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide") {
      apply(configure)
    }

  /** Add the JetBrains Maven Repository ` https ://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies` */
  fun kotlinIdePluginDependencies(
    configure: MavenArtifactRepository.() -> Unit = {}
  ): MavenArtifactRepository =
    repositories.maven(" https ://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies") {
      apply(configure)
    }

  /** Add the JetBrains Maven Repository `https://cache-redirector.jetbrains.com/intellij-dependencies` */
  fun intellijDependencies(
    configure: MavenArtifactRepository.() -> Unit = {}
  ): MavenArtifactRepository =
    repositories.maven("https://cache-redirector.jetbrains.com/intellij-dependencies") {
      apply(configure)
    }

  /** Add the JetBrains Maven Repository `https://www.myget.org/F/rd-snapshots/maven` */
  fun rdSnapshots(
    configure: MavenArtifactRepository.() -> Unit = {}
  ): MavenArtifactRepository =
    repositories.maven("https://www.myget.org/F/rd-snapshots/maven/") {
      apply(configure)
    }
}
//endregion
