package buildsrc.conventions

import buildsrc.settings.MavenPublishingSettings.Companion.mavenPublishing

plugins {
  `maven-publish`
  signing
  id("com.gradleup.nmcp")
  id("buildsrc.conventions.maven-publishing-settings")
}


//region POM convention
publishing {
  publications.withType<MavenPublication>().configureEach {
    pom {
      name.convention("Dokkatoo")
      description.convention("Dokkatoo is a Gradle plugin that generates documentation for your Kotlin projects")
      url.convention("https://adamko-dev.github.io/dokkatoo/")

      scm {
        connection.convention("scm:git:https://github.com/adamko-dev/dokkatoo")
        developerConnection.convention("scm:git:https://github.com/adamko-dev/dokkatoo")
        url.convention("https://github.com/adamko-dev/dokkatoo")
      }

      licenses {
        license {
          name.convention("Apache-2.0")
          url.convention("https://www.apache.org/licenses/LICENSE-2.0.txt")
        }
      }

      developers {
        developer {
          email.set("adam@adamko.dev")
        }
      }
    }
  }
}
//endregion


//region GitHub branch publishing
publishing {
  repositories {
    maven(mavenPublishing.githubPublishDir) {
      name = "GitHubPublish"
    }
  }
}
//endregion


//region Maven Central publishing/signing
publishing {
  repositories {

    val adamkoDevUsername = mavenPublishing.adamkoDevUsername.orNull
    val adamkoDevPassword = mavenPublishing.adamkoDevPassword.orNull
    if (!adamkoDevUsername.isNullOrBlank() && !adamkoDevPassword.isNullOrBlank()) {
      maven(mavenPublishing.adamkoDevReleaseUrl) {
        name = "AdamkoDev"
        credentials {
          username = adamkoDevUsername
          password = adamkoDevPassword
        }
      }
    }
  }

  // com.gradle.plugin-publish automatically adds a Javadoc jar
}

signing {
  logger.info("maven-publishing.gradle.kts enabled signing for ${project.path}")

  val keyId = mavenPublishing.signingKeyId.orNull
  val key = mavenPublishing.signingKey.orNull
  val password = mavenPublishing.signingPassword.orNull

  if (!keyId.isNullOrBlank() && !key.isNullOrBlank() && !password.isNullOrBlank()) {
    useInMemoryPgpKeys(keyId, key, password)
  }

  setRequired({
    gradle.taskGraph.allTasks
      .filterIsInstance<PublishToMavenRepository>()
      .any {
        it.repository.name in setOf(
          "SonatypeRelease",
          "AdamkoDev",
        )
      }
  })
}

//afterEvaluate {
//  com.gradle.plugin-publish automatically signs tasks in a weird way, that stops this from working:
//  signing {
//    sign(publishing.publications)
//  }
//}
//endregion


//region Fix Gradle warning about signing tasks using publishing task outputs without explicit dependencies
// https://youtrack.jetbrains.com/issue/KT-46466 https://github.com/gradle/gradle/issues/26091
tasks.withType<AbstractPublishToMaven>().configureEach {
  val signingTasks = tasks.withType<Sign>()
  mustRunAfter(signingTasks)
}
//endregion


//region publishing logging
tasks.withType<AbstractPublishToMaven>().configureEach {
  val publicationGAV = provider { publication?.run { "$group:$artifactId:$version" } }
  doLast("log publication GAV") {
    if (publicationGAV.isPresent) {
      logger.info("[task: ${path}] ${publicationGAV.get()}")
    }
  }
}
//endregion


//region Maven Central can't handle parallel uploads, so limit parallel uploads with a service.
abstract class MavenPublishLimiter : BuildService<BuildServiceParameters.None>

val mavenPublishLimiter =
  gradle.sharedServices.registerIfAbsent("mavenPublishLimiter", MavenPublishLimiter::class) {
    maxParallelUsages = 1
  }

tasks.withType<PublishToMavenRepository>().configureEach {
  usesService(mavenPublishLimiter)
}
//endregion


//region IJ workarounds
// manually define the Kotlin DSL accessors because IntelliJ _still_ doesn't load them properly
fun Project.publishing(configure: PublishingExtension.() -> Unit): Unit =
  extensions.configure(configure)

val Project.publishing: PublishingExtension
  get() = extensions.getByType()

fun Project.signing(configure: SigningExtension.() -> Unit): Unit =
  extensions.configure(configure)

val Project.signing: SigningExtension
  get() = extensions.getByType()
//endregion
