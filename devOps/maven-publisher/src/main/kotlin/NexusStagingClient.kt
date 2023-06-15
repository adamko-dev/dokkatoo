import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.isSuccess
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay


/**
 * NexusStagingClient will create a staging repository and upload the given files to the given staging repository
 * Creating a staging repository avoid having split artifacts
 *
 * The process is outlined in this document (from 2014!):
 * https://support.sonatype.com/hc/en-us/articles/213465868-Uploading-to-a-Staging-Repository-via-REST-API
 *
 * @param baseUrl: the url of the nexus instance, defaults to the OSSRH url
 * @param username: your Nexus username. For OSSRH, this is your Sonatype jira username
 * @param password: your Nexus password. For OSSRH, this is your Sonatype jira password
 */
class NexusStagingClient(
  private val baseUrl: String = "https://oss.sonatype.org/service/local/",
  username: CharSequence,
  password: CharSequence,
) {

  private val nexusApi = NexusStagingApi(
    baseUrl = baseUrl,
    username = username,
    password = password,
  )

  /**
   * @param directory: the root of a directory containing a maven hierarchy like below.
   *
   * ```text
   * $directory/com/example/module1/version/module1-version.jar
   * $directory/com/example/module1/version/module1-version.jar.md5
   * $directory/com/example/module1/version/module1-version.jar.asc
   * $directory/com/example/module1/version/module1-version.jar.asc.md5
   * $directory/com/example/module1/version/module1-version.pom
   * $directory/com/example/module1/version/module1-version.pom.md5
   * $directory/com/example/module1/version/module1-version.pom.asc
   * $directory/com/example/module1/version/module1-version.pom.asc.md5
   * $directory/com/example/module1/version/module1-version.jar
   * $directory/com/example/module2/version/module2-version.jar
   * etc...
   * ```
   * The directory can contain several modules/versions
   *
   * @param profileId For OSSRH, you will see it at `https://oss.sonatype.org/#stagingProfiles;${profileId}`
   * If you have only one, you can also get it from [getProfiles]
   * @param progress: a callback called for each uploaded file
   */
  suspend fun upload(
    directory: File,
    profileId: String,
    comment: String = "Dokkatoo Staging Repository",
    progress: ((uploadedSize: Long, total: Long, path: String) -> Unit) = { _, _, _ -> }
  ): String {

    val repositoryId = nexusApi.createRepository(profileId, comment).stagedRepositoryId

    check(repositoryId != null) {
      "tried to create staging repository, but response did not contain a stagingRepositoryId"
    }
    val files = directory.walk().filter { it.isFile }

    val totalSize = files.sumOf { Files.size(it.toPath()) }

    files.fold(0L) { uploadedSize, file ->
      val relativePath = file.relativeTo(directory).path
      progress.invoke(uploadedSize, totalSize, relativePath)

      val uploadResponse = nexusApi.uploadFile(
        repositoryId = repositoryId,
        relativePath = relativePath,
        file = file
      )

      check(uploadResponse.status.isSuccess()) {
        "Cannot put ${uploadResponse.request.url}:\n${uploadResponse.bodyAsText()}"
      }

      Files.size(file.toPath())
    }

    return repositoryId
  }

  /**
   * Return a list of all staging repositories
   */
  suspend fun getRepositories(): List<Repository> =
    nexusApi.getRepositories()

  /**
   * Return a specific staging repository
   */
  suspend fun getRepository(repositoryId: String): Repository =
    nexusApi.getRepository(repositoryId)

  /**
   * Closes the given staging repositories.
   *
   * Closing a repository triggers the checks (groupId, pom, signatures, etc...)
   * It is mandatory to close a repository before it can be released.
   */
  suspend fun closeRepositories(repositoryIds: List<String>) {
    nexusApi.closeRepositories(repositoryIds)
  }

  /**
   * Releases the given staging repositories. This is the big "release" button. Once a repository is released, it cannot
   * be removed. Use with care.
   */
  suspend fun releaseRepositories(
    repositoryIds: List<String>,
    dropAfterRelease: Boolean,
  ) {
    nexusApi.releaseRepositories(repositoryIds, dropAfterRelease)
  }

  /**
   * Creates a new staging repository.
   *
   * @param profileId: the profileId used to create the repository
   * @param description: a description of the repository
   *
   * @return the id of the created repository
   */
  suspend fun createRepository(
    profileId: String,
    description: String = "Dokkatoo Staging Repository"
  ): String? {
    return nexusApi.createRepository(profileId, description).stagedRepositoryId
  }

  /**
   * Drops the given staging repositories. This will delete the repositories and all content associated.
   */
  suspend fun dropRepositories(repositoryIds: List<String>) {
    nexusApi.dropRepositories(repositoryIds)
  }

  /**
   * @return the list of all profiles associated with this account
   */
  suspend fun getProfiles(): List<Profile> {
    val profiles = nexusApi.getProfiles()

    require(profiles.isNotEmpty()) { "getProfiles didn't return any data" }

    return profiles
  }

  /**
   * [waitForClose] is a meta API that will use [getRepositories] to check for the status of a repository
   */
  suspend fun waitForClose(
    repositoryId: String,
    pollingInterval: Duration = 15.seconds,
    progress: () -> Unit,
  ) {
    while (true) {
      val repository = nexusApi.getRepository(repositoryId)

      if (repository.type == "closed" && !repository.transitioning) {
        break
      }
      progress()

      delay(pollingInterval)
    }
  }
}
