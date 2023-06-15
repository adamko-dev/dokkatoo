import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.plugins.timeout
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.resources.Resource
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.readChannel
import java.io.File
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


/**
 * Nexus service definition based on:
 * - Nexus STAGING API: https://oss.sonatype.org/nexus-staging-plugin/default/docs/index.html
 * - Nexus CORE API: https://repository.sonatype.org/nexus-restlet1x-plugin/default/docs/index.html
 * - Staging upload: https://support.sonatype.com/hc/en-us/articles/213465868-Uploading-to-a-Staging-Repository-via-REST-API
 * - https://github.com/Codearte/gradle-nexus-staging-plugin
 * - https://github.com/marcphilipp/nexus-publish-plugin
 *
 * OSSRH is using a 2.x version of Nexus so a lot of the more recent 3.x docs do not apply
 */
internal class NexusStagingApi(
  baseUrl: String = "https://oss.sonatype.org/service/local/",
  username: CharSequence,
  password: CharSequence,
  private val client: HttpClient = HttpClient(CIO) {
    install(Resources)
    install(HttpTimeout)
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        isLenient = true
      })
    }

    install(Auth) {
      basic {
        credentials {
          BasicAuthCredentials(username = username.toString(), password = password.toString())
        }
      }
    }
  }
) {

  /** `GET staging/profile_repositories` */
  suspend fun getRepositories(): List<Repository> =
    client.get(NexusStaging.GetProfileRepositories()).body()

  /** `GET staging/repository/{repositoryId}` */
  suspend fun getRepository(repositoryId: String): Repository =
    client.get(NexusStaging.Repository.Get(repositoryId)) {
      expectSuccess = false
    }.body()

  /** `POST staging/bulk/close` */
  suspend fun closeRepositories(
    repositoryIds: List<String>,
  ) {
    val input = BulkRepositoryData(
      repositoryIds = repositoryIds,
    )
    client.post(NexusStaging.Bulk.Close()) {
      contentType(ContentType.Application.Json)
      setBody(input)
    }
  }

  /** `POST staging/bulk/promote` */
  suspend fun releaseRepositories(
    repositoryIds: List<String>,
    dropAfterRelease: Boolean? = null,
  ) {
    val input = BulkRepositoryData(
      repositoryIds = repositoryIds,
      dropAfterRelease = dropAfterRelease
    )
    client.post(NexusStaging.Bulk.Promote()) {
      contentType(ContentType.Application.Json)
      setBody(input)
    }
  }

  /** `POST staging/bulk/drop` */
  suspend fun dropRepositories(
    repositoryIds: List<String>,
  ) {
    val input = BulkRepositoryData(
      repositoryIds = repositoryIds,
    )
    client.post(NexusStaging.Bulk.Drop()) {
      contentType(ContentType.Application.Json)
      setBody(input)
    }
  }

  /** `GET staging/profiles` */
  suspend fun getProfiles(): List<Profile> =
    client.get(NexusStaging.Profiles).body()

  /** `POST staging/profiles/{stagingProfileId}/start` */
  suspend fun createRepository(
    profileId: String,
    description: String,
  ): CreatedRepository {
    return client.post(NexusStaging.Profiles.CreateRepository(profileId = profileId)) {
      contentType(ContentType.Application.Json)
      setBody(Description(description = description))
      timeout {
        // Opening a staging repository can take 2-3 minutes
        requestTimeoutMillis = 3.minutes.inWholeMilliseconds
        connectTimeoutMillis = 6.minutes.inWholeMilliseconds
      }
    }.body()
  }

  /** `POST staging/deployByRepositoryId/{repositoryId}/{relativePath}` */
  suspend fun uploadFile(
    repositoryId: String,
    relativePath: String,
    file: File,
  ): HttpResponse {
    return client.put(
      NexusStaging.DeployByRepositoryId(
        repositoryId,
        relativePath,
      )
    ) {
      expectSuccess = false
      contentType(ContentType.Application.OctetStream)
      setBody(file.readChannel())
    }
  }
}


@Resource("/staging")
private class NexusStaging {
  @Resource("profile_repositories")
  class GetProfileRepositories

  @Resource("repository")
  class Repository {
    @Resource("{repositoryId}")
    class Get(val repositoryId: String)
  }

  @Resource("deployByRepositoryId/{repositoryId}/{relativePath}")
  class DeployByRepositoryId(
    val repositoryId: String,
    val relativePath: String,
  )

  @Resource("bulk")
  class Bulk {
    @Resource("close")
    class Close
    @Resource("promote")
    class Promote
    @Resource("drop")
    class Drop
  }

  @Resource("profiles")
  class Profiles {

    @Resource("{profileId}/start")
    class CreateRepository(
      val profileId: String,
    )
  }
}


@Serializable
internal data class CreatedRepository(
  val data: Data
) {
  val stagedRepositoryId: String? by data::stagedRepositoryId

  @Serializable
  data class Data(
    val stagedRepositoryId: String?
  )
}

@Serializable
private data class Description(
  val data: Data
) {
  constructor(description: String) : this(Data(description = description))

  @Serializable
  data class Data(
    val description: String,
  )
}

@Serializable
private data class BulkRepositoryData(
  val data: Data
) {
  constructor(
    repositoryIds: List<String>,
    dropAfterRelease: Boolean? = null,
  ) : this(
    Data(
      stagedRepositoryIds = repositoryIds,
      autoDropAfterRelease = dropAfterRelease,
    )
  )

  @Serializable
  data class Data(
    val stagedRepositoryIds: List<String>,
    val autoDropAfterRelease: Boolean? = null
  )
}

@Serializable
class Profile(val id: String, val name: String)

@Serializable
class Repository(
  val repositoryId: String,
  val transitioning: Boolean,
  val type: String,
  val description: String?,
)

//@JsonClass(generateAdapter = true)
//class Data<T>(val data: T)
//
//@JsonClass(generateAdapter = true)
//class Profile(val id: String, val name: String)
//
//@JsonClass(generateAdapter = true)
//class Description(val description: String)
//
//@JsonClass(generateAdapter = true)
//class CreatedRepository(var stagedRepositoryId: String)
//
//@JsonClass(generateAdapter = true)
//class Repository(
//  val repositoryId: String,
//  val transitioning: Boolean,
//  val type: String,
//  val description: String?
//)
//
//@JsonClass(generateAdapter = true)
//class TransitionRepositoryInput(
//  val stagedRepositoryIds: List<String>,
//  val autoDropAfterRelease: Boolean? = null
//)
//
//fun NexusApi(
//  username: String,
//  password: String,
//  baseUrl: String = "https://oss.sonatype.org/service/local/",
//) = NexusApi(OkHttpClient(username, password), baseUrl)
//
//fun NexusApi(
//  okHttpClient: OkHttpClient,
//  baseUrl: String = "https://oss.sonatype.org/service/local/",
//): NexusStagingApi {
//  val retrofit = Retrofit.Builder()
//    .baseUrl(baseUrl)
//    .client(okHttpClient)
//    .addConverterFactory(MoshiConverterFactory.create())
//    .build()
//
//  return retrofit.create(NexusStagingApi::class.java)
//}
//
//fun OkHttpClient(username: String, password: String) =
//  OkHttpClient.Builder().addInterceptor { chain ->
//    val builder = chain.request().newBuilder()
//    builder.addHeader("Authorization", Credentials.basic(username, password))
//    builder.addHeader("Accept", "application/json")
//    builder.addHeader("Content-Type", "application/json")
//    builder.addHeader("User-Agent", "vespene")
//    chain.proceed(builder.build())
//  }
//    .readTimeout(600, TimeUnit.SECONDS) // Opening a staging repository can take 2-3 minutes
//    .connectTimeout(600, TimeUnit.SECONDS)
//    .build()
