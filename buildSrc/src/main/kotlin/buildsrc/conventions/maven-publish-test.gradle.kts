package buildsrc.conventions


/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

Utility for publishing a project to a local Maven directory for use in integration tests.



* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

abstract class MavenPublishTest {
  abstract val testMavenRepo: DirectoryProperty
  internal abstract val testMavenRepoTemp: DirectoryProperty
}

val mavenPublishTest = extensions.create<MavenPublishTest>("mavenPublishTest").apply {
  testMavenRepo.convention(layout.buildDirectory.dir("test-maven-repo"))
  testMavenRepoTemp.convention(layout.buildDirectory.dir("tmp/test-maven-repo"))
}


val publishToTestMavenRepo by tasks.registering(Sync::class) {
  group = PublishingPlugin.PUBLISH_TASK_GROUP
  description = "Publishes all Maven publications to the test Maven repository."
  from(mavenPublishTest.testMavenRepoTemp)
  into(mavenPublishTest.testMavenRepo)
}



plugins.withType<MavenPublishPlugin>().all {
  extensions
    .getByType<PublishingExtension>()
    .publications
    .withType<MavenPublication>().all publication@{
      val publicationName = this@publication.name
      val installTaskName = "publish${publicationName.capitalize()}PublicationToTestMavenRepo"

      // Register a publiction task for each publication.
      // Use PublishToMavenLocal, because the PublishToMavenRepository task will *always* create
      // a new jar, even if nothing has changed, and append a timestamp, which results in a large
      // directory. PublishToMavenLocal does not append a timestamp, so the target directory is smaller
      val installTask = tasks.register<PublishToMavenLocal>(installTaskName) {
        description = "Publishes Maven publication '$publicationName' to the test Maven repository."
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        outputs.cacheIf { true }
        publication = this@publication
        val destinationDir = mavenPublishTest.testMavenRepoTemp.asFile
        inputs.property("testMavenRepoTempDir", destinationDir.map { it.invariantSeparatorsPath })
        doFirst {
          /**
           * `maven.repo.local` will set the destination directry
           *
           * @see org.gradle.api.internal.artifacts.mvnsettings.DefaultLocalMavenRepositoryLocator.getLocalMavenRepository
           */
          System.setProperty("maven.repo.local", destinationDir.get().absolutePath)
        }
      }

      publishToTestMavenRepo.configure {
        dependsOn(installTask)
      }
    }
}
