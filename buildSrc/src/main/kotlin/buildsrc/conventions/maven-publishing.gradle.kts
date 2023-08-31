package buildsrc.conventions

import buildsrc.settings.MavenPublishingSettings
import org.gradle.api.plugins.JavaBasePlugin.DOCUMENTATION_GROUP

plugins {
  `maven-publish`
  signing
}

val mavenPublishing =
  extensions.create<MavenPublishingSettings>(MavenPublishingSettings.EXTENSION_NAME, project)


//region POM convention
publishing {
  publications.withType<MavenPublication>().configureEach {
    pom {
      name.convention("Dokkatoo")
      description.convention("Dokkatoo is a Gradle plugin that generates documentation for your Kotlin projects")
      url.convention("https://github.com/adamko-dev/dokkatoo")

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
val javadocJarStub by tasks.registering(Jar::class) {
  group = DOCUMENTATION_GROUP
  description = "Empty Javadoc Jar (required by Maven Central)"
  archiveClassifier.set("javadoc")
}

publishing {
  repositories {
    if (mavenPublishing.mavenCentralCredentials.isPresent) {
      maven(mavenPublishing.sonatypeReleaseUrl) {
        name = "SonatypeRelease"
        credentials(mavenPublishing.mavenCentralCredentials.get())
      }
    }
  }

  // Maven Central requires Javadoc JAR, which this project doesn't have because it's not Java, so use an empty jar.
  publications.withType<MavenPublication>().configureEach {
    artifact(javadocJarStub)
  }
}

signing {
  logger.info("maven-publishing.gradle.kts enabled signing for ${project.path}")

  val keyId = mavenPublishing.signingKeyId.orNull
  val key = mavenPublishing.signingKey.orNull
  val password = mavenPublishing.signingPassword.orNull

  if (keyId != null && key != null && password != null) {
    useInMemoryPgpKeys(keyId, key, password)
  }

  setRequired({
    keyId != null && key != null && password != null
  })
}

afterEvaluate {
  // Register signatures in afterEvaluate, otherwise the signing plugin creates
  // the signing tasks too early, before all the publications are added.
  signing {
    sign(publishing.publications)
  }
}
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
      logger.lifecycle("[task: ${path}] ${publicationGAV.get()}")
    }
  }
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
